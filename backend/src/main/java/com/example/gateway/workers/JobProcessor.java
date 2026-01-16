package com.example.gateway.workers;

import com.example.gateway.jobs.DeliverWebhookJob;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.ProcessPaymentJob;
import com.example.gateway.jobs.ProcessRefundJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@EnableScheduling
public class JobProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(JobProcessor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private PaymentWorker paymentWorker;
    
    @Autowired
    private WebhookWorker webhookWorker;
    
    @Autowired
    private RefundWorker refundWorker;
    
    /**
     * Process payment jobs every 500ms
     */
    @Scheduled(fixedDelay = 500, initialDelay = 2000)
    public void processPaymentJobs() {
        try {
            Set<String> jobIds = redisTemplate.opsForSet().members(JobConstants.PAYMENT_QUEUE);
            if (jobIds != null && !jobIds.isEmpty()) {
                for (String jobId : jobIds) {
                    try {
                        String jobData = redisTemplate.opsForValue().get("job:" + jobId);
                        if (jobData != null) {
                            ProcessPaymentJob job = objectMapper.readValue(jobData, ProcessPaymentJob.class);
                            logger.debug("Processing payment job: {}", jobId);
                            paymentWorker.processPayment(job);
                            // Remove from queue after processing
                            redisTemplate.opsForSet().remove(JobConstants.PAYMENT_QUEUE, jobId);
                            redisTemplate.delete("job:" + jobId);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing payment job: {}", jobId, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in payment job processor", e);
        }
    }
    
    /**
     * Process webhook delivery jobs every 500ms
     */
    @Scheduled(fixedDelay = 500, initialDelay = 2500)
    public void processWebhookJobs() {
        try {
            Set<String> jobIds = redisTemplate.opsForSet().members(JobConstants.WEBHOOK_QUEUE);
            if (jobIds != null && !jobIds.isEmpty()) {
                for (String jobId : jobIds) {
                    try {
                        String jobData = redisTemplate.opsForValue().get("job:" + jobId);
                        if (jobData != null) {
                            DeliverWebhookJob job = objectMapper.readValue(jobData, DeliverWebhookJob.class);
                            logger.debug("Processing webhook delivery job: {}", jobId);
                            webhookWorker.deliverWebhook(job);
                            // Remove from queue after processing
                            redisTemplate.opsForSet().remove(JobConstants.WEBHOOK_QUEUE, jobId);
                            redisTemplate.delete("job:" + jobId);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing webhook job: {}", jobId, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in webhook job processor", e);
        }
    }
    
    /**
     * Process refund jobs every 500ms
     */
    @Scheduled(fixedDelay = 500, initialDelay = 3000)
    public void processRefundJobs() {
        try {
            Set<String> jobIds = redisTemplate.opsForSet().members(JobConstants.REFUND_QUEUE);
            if (jobIds != null && !jobIds.isEmpty()) {
                for (String jobId : jobIds) {
                    try {
                        String jobData = redisTemplate.opsForValue().get("job:" + jobId);
                        if (jobData != null) {
                            ProcessRefundJob job = objectMapper.readValue(jobData, ProcessRefundJob.class);
                            logger.debug("Processing refund job: {}", jobId);
                            refundWorker.processRefund(job);
                            // Remove from queue after processing
                            redisTemplate.opsForSet().remove(JobConstants.REFUND_QUEUE, jobId);
                            redisTemplate.delete("job:" + jobId);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing refund job: {}", jobId, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in refund job processor", e);
        }
    }
}
