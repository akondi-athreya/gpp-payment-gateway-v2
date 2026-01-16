package com.example.gateway.workers;

import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.ProcessPaymentJob;
import com.example.gateway.models.Payment;
import com.example.gateway.models.PaymentMethod;
import com.example.gateway.repositories.PaymentRepository;
import com.example.gateway.services.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.Random;

@Service
public class PaymentWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentWorker.class);
    private static final Random random = new Random();
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private WebhookService webhookService;
    
    @Value("${test.mode:false}")
    private boolean testMode;
    
    @Value("${test.payment.success:true}")
    private boolean testPaymentSuccess;
    
    @Value("${test.processing.delay:1000}")
    private long testProcessingDelay;
    
    // Success rates for different payment methods
    private static final double UPI_SUCCESS_RATE = 0.90;
    private static final double CARD_SUCCESS_RATE = 0.95;
    
    public void processPayment(ProcessPaymentJob job) {
        logger.info("Starting to process payment job: {}", job.getJobId());
        
        try {
            // Step 1: Fetch payment from database
            Optional<Payment> paymentOpt = paymentRepository.findById(job.getPaymentId());
            if (!paymentOpt.isPresent()) {
                logger.error("Payment not found for ID: {}", job.getPaymentId());
                return;
            }
            
            Payment payment = paymentOpt.get();
            
            // Step 2: Simulate processing delay
            simulateProcessingDelay();
            
            // Step 3: Determine payment outcome
            boolean isSuccess = determinePaymentOutcome(payment);
            
            // Step 4: Update payment status based on outcome
            if (isSuccess) {
                payment.setStatus("success");
                logger.info("Payment {} processed successfully", payment.getId());
            } else {
                payment.setStatus("failed");
                payment.setErrorCode("PAYMENT_FAILED");
                payment.setErrorDescription("Payment processing failed due to payment gateway error");
                logger.info("Payment {} failed", payment.getId());
            }
            
            paymentRepository.save(payment);
            
            // Step 5: Enqueue webhook delivery job for appropriate event
            String event = isSuccess ? "payment.success" : "payment.failed";
            logger.info("Enqueueing webhook for event: {} for payment: {}", event, payment.getId());
            webhookService.enqueueWebhookDelivery(
                    payment.getMerchant().getId(),
                    event,
                    payment,
                    null
            );
            
            logger.info("Payment processing job completed: {}", job.getJobId());
            
        } catch (Exception e) {
            logger.error("Error processing payment job: " + job.getJobId(), e);
            throw new RuntimeException("Failed to process payment: " + job.getPaymentId(), e);
        }
    }
    
    private void simulateProcessingDelay() {
        try {
            if (testMode) {
                // Use test mode delay
                Thread.sleep(testProcessingDelay);
                logger.debug("Test mode processing delay: {}ms", testProcessingDelay);
            } else {
                // Random delay between 5-10 seconds
                long delayMs = 5000 + random.nextInt(5001);
                Thread.sleep(delayMs);
                logger.debug("Production mode processing delay: {}ms", delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Payment processing delay interrupted", e);
        }
    }
    
    private boolean determinePaymentOutcome(Payment payment) {
        if (testMode) {
            // In test mode, use TEST_PAYMENT_SUCCESS flag
            return testPaymentSuccess;
        }
        
        // Production mode: use success rates based on payment method
        PaymentMethod method = payment.getMethod();
        double successRate = PaymentMethod.UPI == method ? UPI_SUCCESS_RATE : CARD_SUCCESS_RATE;
        
        double randomValue = random.nextDouble();
        boolean isSuccess = randomValue < successRate;
        
        logger.debug("Payment outcome determination - Method: {}, SuccessRate: {}, RandomValue: {}, Result: {}",
                method, successRate, randomValue, isSuccess);
        
        return isSuccess;
    }
}
