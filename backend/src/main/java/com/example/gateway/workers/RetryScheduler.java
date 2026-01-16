package com.example.gateway.workers;

import com.example.gateway.jobs.DeliverWebhookJob;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.models.WebhookLog;
import com.example.gateway.repositories.WebhookLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RetryScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryScheduler.class);
    
    @Autowired
    private WebhookLogRepository webhookLogRepository;
    
    @Autowired
    private JobServiceImpl jobService;
    
    /**
     * Check for pending webhooks that are ready to retry
     * Runs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void scheduleWebhookRetries() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            
            // Find all pending webhooks that are ready for retry
            List<WebhookLog> pendingRetries = webhookLogRepository.findPendingRetries(now);
            
            if (pendingRetries != null && !pendingRetries.isEmpty()) {
                logger.info("Found {} webhooks ready for retry", pendingRetries.size());
                
                for (WebhookLog webhookLog : pendingRetries) {
                    try {
                        // Re-enqueue the delivery job
                        String jobId = webhookLog.getId();
                        DeliverWebhookJob job = new DeliverWebhookJob(
                                jobId,
                                webhookLog.getMerchant().getId(),
                                webhookLog.getEvent(),
                                webhookLog.getPayload()
                        );
                        
                        jobService.enqueueJob(JobConstants.WEBHOOK_QUEUE, job, jobId);
                        logger.debug("Re-enqueued webhook retry job: {} for merchant: {}, attempt: {}",
                                jobId, webhookLog.getMerchant().getId(), webhookLog.getAttempts());
                    } catch (Exception e) {
                        logger.error("Error re-enqueueing webhook retry job: {}", webhookLog.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in webhook retry scheduler", e);
        }
    }
}
