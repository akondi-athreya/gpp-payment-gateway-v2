package com.example.gateway.services;

import com.example.gateway.jobs.DeliverWebhookJob;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import com.example.gateway.models.WebhookLog;
import com.example.gateway.repositories.MerchantRepository;
import com.example.gateway.repositories.WebhookLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookServiceImpl implements WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookServiceImpl.class);
    
    @Autowired
    private WebhookPayloadBuilder payloadBuilder;
    
    @Autowired
    private WebhookLogRepository webhookLogRepository;
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    @Autowired
    private JobServiceImpl jobService;
    
    @Value("${webhook.retry.test.mode:false}")
    private boolean webhookRetryTestMode;
    
    @Override
    public void enqueueWebhookDelivery(UUID merchantId, String event, Payment payment, Refund refund) {
        try {
            // Build payload based on event type
            JsonNode payload;
            if (payment != null) {
                payload = payloadBuilder.buildPaymentPayload(event, payment);
            } else if (refund != null) {
                payload = payloadBuilder.buildRefundPayload(event, refund);
            } else {
                logger.warn("No payment or refund provided for webhook event: {}", event);
                return;
            }
            
            // Create webhook log entry
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setId(UUID.randomUUID().toString());
            webhookLog.setEvent(event);
            webhookLog.setPayload(payload);
            webhookLog.setStatus(JobConstants.JOB_STATUS_PENDING);
            webhookLog.setAttempts(0);
            webhookLog.setCreatedAt(OffsetDateTime.now());
            
            // Fetch merchant
            Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
            if (!merchantOpt.isPresent()) {
                logger.error("Merchant not found for ID: {}", merchantId);
                return;
            }
            
            Merchant merchant = merchantOpt.get();
            webhookLog.setMerchant(merchant);
            
            // Save webhook log
            webhookLogRepository.save(webhookLog);
            logger.info("Created webhook log: {} for event: {} and merchant: {}", webhookLog.getId(), event, merchantId);
            
            // Create and enqueue delivery job
            String jobId = "wh_" + UUID.randomUUID().toString().substring(0, 16);
            DeliverWebhookJob job = new DeliverWebhookJob(jobId, merchantId, event, payload);
            
            jobService.enqueueJob(JobConstants.WEBHOOK_QUEUE, job, jobId);
            logger.info("Enqueued webhook delivery job: {} for merchant: {}, event: {}", jobId, merchantId, event);
            
        } catch (Exception e) {
            logger.error("Error enqueueing webhook delivery for merchant: " + merchantId + ", event: " + event, e);
        }
    }
    
    /**
     * Get webhook retry delay based on attempt number
     */
    public int getRetryDelaySeconds(int attemptNumber) {
        if (webhookRetryTestMode) {
            if (attemptNumber >= JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS_TEST.length) {
                return JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS_TEST[JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS_TEST.length - 1];
            }
            return JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS_TEST[attemptNumber];
        } else {
            if (attemptNumber >= JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS.length) {
                return JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS[JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS.length - 1];
            }
            return JobConstants.WEBHOOK_RETRY_DELAYS_SECONDS[attemptNumber];
        }
    }
    
    /**
     * Calculate next retry time based on attempt number
     */
    public OffsetDateTime calculateNextRetryTime(int attemptNumber) {
        int delaySeconds = getRetryDelaySeconds(attemptNumber);
        return OffsetDateTime.now().plusSeconds(delaySeconds);
    }
}
