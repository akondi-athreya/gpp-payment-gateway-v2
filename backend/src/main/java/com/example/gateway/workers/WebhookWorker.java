package com.example.gateway.workers;

import com.example.gateway.jobs.DeliverWebhookJob;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.WebhookLog;
import com.example.gateway.repositories.MerchantRepository;
import com.example.gateway.repositories.WebhookLogRepository;
import com.example.gateway.services.WebhookServiceImpl;
import com.example.gateway.services.WebhookSignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class WebhookWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookWorker.class);
    private static final int WEBHOOK_TIMEOUT_SECONDS = 5;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private WebhookLogRepository webhookLogRepository;
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    @Autowired
    private WebhookSignatureService signatureService;
    
    @Autowired
    private WebhookServiceImpl webhookService;
    
    @Autowired
    private JobServiceImpl jobService;
    
    public void deliverWebhook(DeliverWebhookJob job) {
        logger.info("Starting webhook delivery job: {}", job.getJobId());
        
        try {
            // This is a simplified version - in production, you'd fetch webhook log by ID
            // For now, we'll create a new delivery attempt
            
            // Fetch merchant details
            Optional<Merchant> merchantOpt = merchantRepository.findById(job.getMerchantId());
            if (!merchantOpt.isPresent()) {
                logger.error("Merchant not found for ID: {}", job.getMerchantId());
                return;
            }
            
            Merchant merchant = merchantOpt.get();
            
            // Check if webhook URL is configured
            if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isEmpty()) {
                logger.warn("Webhook URL not configured for merchant: {}", job.getMerchantId());
                return;
            }
            
            // Check if webhook secret is configured
            if (merchant.getWebhookSecret() == null || merchant.getWebhookSecret().isEmpty()) {
                logger.warn("Webhook secret not configured for merchant: {}", job.getMerchantId());
                return;
            }
            
            // Create or fetch webhook log (simplified - create new entry for this delivery)
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setId(job.getJobId());
            webhookLog.setMerchant(merchant);
            webhookLog.setEvent(job.getEvent());
            webhookLog.setPayload(job.getPayload());
            webhookLog.setStatus(JobConstants.JOB_STATUS_PENDING);
            webhookLog.setAttempts(0);
            webhookLog.setCreatedAt(OffsetDateTime.now());
            
            // Deliver webhook
            deliverWebhookAttempt(merchant, webhookLog, job);
            
            logger.info("Webhook delivery job completed: {}", job.getJobId());
            
        } catch (Exception e) {
            logger.error("Error delivering webhook job: " + job.getJobId(), e);
            throw new RuntimeException("Failed to deliver webhook", e);
        }
    }
    
    private void deliverWebhookAttempt(Merchant merchant, WebhookLog webhookLog, DeliverWebhookJob job) {
        try {
            // Generate HMAC signature
            String payloadString = objectMapper.writeValueAsString(webhookLog.getPayload());
            String signature = signatureService.generateSignature(payloadString, merchant.getWebhookSecret());
            
            // Send HTTP POST request
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(merchant.getWebhookUrl());
            
            // Set headers
            httpPost.setHeader("X-Webhook-Signature", signature);
            httpPost.setHeader("Content-Type", "application/json");
            
            // Set body
            httpPost.setEntity(new StringEntity(payloadString, ContentType.APPLICATION_JSON));
            
            // Execute with timeout
            int socketTimeoutMs = WEBHOOK_TIMEOUT_SECONDS * 1000;
            HttpResponse response = httpClient.execute(httpPost, (httpResponse) -> {
                WebhookLog log = new WebhookLog();
                log.setMerchant(merchant);
                log.setEvent(job.getEvent());
                log.setPayload(job.getPayload());
                log.setAttempts(webhookLog.getAttempts() + 1);
                log.setLastAttemptAt(OffsetDateTime.now());
                
                int statusCode = httpResponse.getCode();
                log.setResponseCode(statusCode);
                
                // Capture response body
                if (httpResponse.getEntity() != null) {
                    try {
                        String responseBody = new BufferedReader(
                                new InputStreamReader(httpResponse.getEntity().getContent())
                        ).readLine();
                        log.setResponseBody(responseBody);
                    } catch (IOException e) {
                        logger.debug("Could not read response body", e);
                    }
                }
                
                // Check if successful (200-299)
                if (statusCode >= 200 && statusCode < 300) {
                    log.setStatus("success");
                    logger.info("Webhook delivered successfully to merchant: {}, event: {}", 
                            merchant.getId(), job.getEvent());
                } else {
                    log.setStatus("pending");
                    
                    // Schedule retry if attempts < max
                    if (log.getAttempts() < JobConstants.MAX_WEBHOOK_ATTEMPTS) {
                        OffsetDateTime nextRetryTime = webhookService.calculateNextRetryTime(log.getAttempts());
                        log.setNextRetryAt(nextRetryTime);
                        logger.info("Webhook delivery failed with status {}. Scheduled retry at: {}", 
                                statusCode, nextRetryTime);
                    } else {
                        log.setStatus("failed");
                        logger.warn("Webhook delivery failed after {} attempts for merchant: {}, event: {}", 
                                JobConstants.MAX_WEBHOOK_ATTEMPTS, merchant.getId(), job.getEvent());
                    }
                }
                
                // Save webhook log
                webhookLogRepository.save(log);
                
                // If retry scheduled, re-enqueue job
                if ("pending".equals(log.getStatus()) && log.getNextRetryAt() != null) {
                    // Job will be picked up by scheduler based on next_retry_at
                    logger.info("Webhook will be retried at: {}", log.getNextRetryAt());
                }
                
                return httpResponse;
            });
            
        } catch (Exception e) {
            logger.error("Error attempting webhook delivery to: " + merchant.getWebhookUrl(), e);
            
            // Log the failed attempt
            WebhookLog failedLog = new WebhookLog();
            failedLog.setMerchant(merchant);
            failedLog.setEvent(job.getEvent());
            failedLog.setPayload(job.getPayload());
            failedLog.setAttempts(webhookLog.getAttempts() + 1);
            failedLog.setLastAttemptAt(OffsetDateTime.now());
            failedLog.setResponseBody(e.getMessage());
            
            if (failedLog.getAttempts() < JobConstants.MAX_WEBHOOK_ATTEMPTS) {
                failedLog.setStatus("pending");
                OffsetDateTime nextRetryTime = webhookService.calculateNextRetryTime(failedLog.getAttempts());
                failedLog.setNextRetryAt(nextRetryTime);
            } else {
                failedLog.setStatus("failed");
            }
            
            webhookLogRepository.save(failedLog);
        }
    }
}
