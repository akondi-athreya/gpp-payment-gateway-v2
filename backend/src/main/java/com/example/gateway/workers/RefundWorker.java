package com.example.gateway.workers;

import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.ProcessRefundJob;
import com.example.gateway.models.Refund;
import com.example.gateway.repositories.RefundRepository;
import com.example.gateway.services.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class RefundWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(RefundWorker.class);
    private static final Random random = new Random();
    
    @Autowired
    private RefundRepository refundRepository;
    
    @Autowired
    private WebhookService webhookService;
    
    public void processRefund(ProcessRefundJob job) {
        logger.info("Starting to process refund job: {}", job.getJobId());
        
        try {
            // Step 1: Fetch refund from database
            Optional<Refund> refundOpt = refundRepository.findById(job.getRefundId());
            if (!refundOpt.isPresent()) {
                logger.error("Refund not found for ID: {}", job.getRefundId());
                return;
            }
            
            Refund refund = refundOpt.get();
            
            // Step 2: Verify payment is refundable (status='success')
            if (!refund.getPayment().getStatus().equals("success")) {
                logger.error("Cannot refund payment with status: {}. Payment must be in 'success' status",
                        refund.getPayment().getStatus());
                refund.setStatus("failed");
                refundRepository.save(refund);
                return;
            }
            
            // Step 3: Verify total refunded amount does not exceed payment amount
            List<Refund> existingRefunds = refundRepository.findByPaymentId(refund.getPayment().getId());
            long totalRefunded = 0;
            for (Refund r : existingRefunds) {
                if (!"failed".equals(r.getStatus())) {
                    totalRefunded += r.getAmount();
                }
            }
            
            if (totalRefunded > refund.getPayment().getAmount()) {
                logger.error("Total refund amount {} exceeds payment amount {}", 
                        totalRefunded, refund.getPayment().getAmount());
                refund.setStatus("failed");
                refundRepository.save(refund);
                return;
            }
            
            // Step 4: Simulate refund processing delay (3-5 seconds)
            simulateRefundProcessingDelay();
            
            // Step 5: Update refund status to 'processed'
            refund.setStatus("processed");
            refund.setProcessedAt(OffsetDateTime.now());
            refundRepository.save(refund);
            logger.info("Refund {} processed successfully", refund.getId());
            
            // Step 6: Enqueue webhook delivery job for 'refund.processed' event
            logger.info("Enqueueing webhook for event: refund.processed for refund: {}", refund.getId());
            webhookService.enqueueWebhookDelivery(
                    refund.getMerchant().getId(),
                    "refund.processed",
                    null,
                    refund
            );
            
            logger.info("Refund processing job completed: {}", job.getJobId());
            
        } catch (Exception e) {
            logger.error("Error processing refund job: " + job.getJobId(), e);
            throw new RuntimeException("Failed to process refund: " + job.getRefundId(), e);
        }
    }
    
    private void simulateRefundProcessingDelay() {
        try {
            // Random delay between 3-5 seconds
            long delayMs = 3000 + random.nextInt(2001);
            Thread.sleep(delayMs);
            logger.debug("Refund processing delay: {}ms", delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Refund processing delay interrupted", e);
        }
    }
}
