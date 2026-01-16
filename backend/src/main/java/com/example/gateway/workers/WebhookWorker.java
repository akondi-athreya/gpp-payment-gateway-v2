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
            // Fetch existing webhook log by ID, or create new one
            WebhookLog webhookLog = webhookLogRepository.findById(job.getJobId()).orElse(null);
            
            if (webhookLog == null) {
                // First attempt - create webhook log entry
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
                
                // Create new webhook log
                webhookLog = new WebhookLog();
                webhookLog.setId(job.getJobId());
                webhookLog.setMerchant(merchant);
                webhookLog.setEvent(job.getEvent());
                webhookLog.setPayload(job.getPayload());
                webhookLog.setStatus(JobConstants.JOB_STATUS_PENDING);
                webhookLog.setAttempts(0);
                webhookLog.setCreatedAt(OffsetDateTime.now());
            }
            
            // Deliver webhook
            deliverWebhookAttempt(webhookLog, job);
            
            logger.info("Webhook delivery job completed: {}", job.getJobId());
            
        } catch (Exception e) {
            logger.error("Error delivering webhook job: " + job.getJobId(), e);
            throw new RuntimeException("Failed to deliver webhook", e);
        }
    }
    
    private void deliverWebhookAttempt(WebhookLog webhookLog, DeliverWebhookJob job) {
        try {
            Merchant merchant = webhookLog.getMerchant();
            
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
                int statusCode = httpResponse.getCode();
                webhookLog.setAttempts(webhookLog.getAttempts() + 1);
                webhookLog.setLastAttemptAt(OffsetDateTime.now());
                webhookLog.setResponseCode(statusCode);
                
                // Capture response body
                if (httpResponse.getEntity() != null) {
                    try {
                        String responseBody = new BufferedReader(
                                new InputStreamReader(httpResponse.getEntity().getContent())
                        ).readLine();
                        webhookLog.setResponseBody(responseBody);
                    } catch (IOException e) {
                        logger.debug("Could not read response body", e);
                    }
                }
                
                // Check if successful (200-299)
                if (statusCode >= 200 && statusCode < 300) {
                    webhookLog.setStatus("success");
                    logger.info("Webhook delivered successfully to merchant: {}, event: {}", 
                            merchant.getId(), job.getEvent());
                } else {
                    // Schedule retry if attempts < max
                    if (webhookLog.getAttempts() < JobConstants.MAX_WEBHOOK_ATTEMPTS) {
                        webhookLog.setStatus("pending");
                        OffsetDateTime nextRetryTime = webhookService.calculateNextRetryTime(webhookLog.getAttempts());
                        webhookLog.setNextRetryAt(nextRetryTime);
                        logger.info("Webhook delivery failed with status {}. Scheduled retry at: {}", 
                                statusCode, nextRetryTime);
                    } else {
                        webhookLog.setStatus("failed");
                        logger.warn("Webhook delivery failed after {} attempts for merchant: {}, event: {}", 
                                JobConstants.MAX_WEBHOOK_ATTEMPTS, merchant.getId(), job.getEvent());
                    }
                }
                
                // Save webhook log
                webhookLogRepository.save(webhookLog);
                
                // If retry scheduled, re-enqueue job
                if ("pending".equals(webhookLog.getStatus()) && webhookLog.getNextRetryAt() != null) {
                    // Job will be picked up by scheduler based on next_retry_at
                    logger.info("Webhook will be retried at: {}", webhookLog.getNextRetryAt());
                }
                
                return httpResponse;
            });
            
        } catch (Exception e) {
            logger.error("Error attempting webhook delivery to: " + webhookLog.getMerchant().getWebhookUrl(), e);
            
            // Log the failed attempt
            webhookLog.setAttempts(webhookLog.getAttempts() + 1);
            webhookLog.setLastAttemptAt(OffsetDateTime.now());
            webhookLog.setResponseBody(e.getMessage());
            
            if (webhookLog.getAttempts() < JobConstants.MAX_WEBHOOK_ATTEMPTS) {
                webhookLog.setStatus("pending");
                OffsetDateTime nextRetryTime = webhookService.calculateNextRetryTime(webhookLog.getAttempts());
                webhookLog.setNextRetryAt(nextRetryTime);
            } else {
                webhookLog.setStatus("failed");
            }
            
            webhookLogRepository.save(webhookLog);
        }
    }
}
