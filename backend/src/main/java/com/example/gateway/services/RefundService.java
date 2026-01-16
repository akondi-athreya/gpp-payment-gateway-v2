package com.example.gateway.services;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import com.example.gateway.repositories.MerchantRepository;
import com.example.gateway.repositories.PaymentRepository;
import com.example.gateway.repositories.RefundRepository;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.jobs.ProcessRefundJob;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final JobServiceImpl jobService;

    public RefundService(RefundRepository refundRepository, PaymentRepository paymentRepository,
            MerchantRepository merchantRepository, JobServiceImpl jobService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.merchantRepository = merchantRepository;
        this.jobService = jobService;
    }

    public Refund createRefund(String apiKey, String apiSecret, String paymentId, 
            long amount, String notes) {
        
        // Validate credentials
        Optional<Merchant> merchantOpt = merchantRepository.findByApiKeyAndApiSecret(apiKey, apiSecret);
        if (merchantOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid API credentials");
        }
        
        Merchant merchant = merchantOpt.get();
        
        // Validate payment exists
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found");
        }
        
        Payment payment = paymentOpt.get();
        
        // Validate payment status is success
        if (!"success".equals(payment.getStatus())) {
            throw new IllegalArgumentException("Can only refund successful payments");
        }
        
        // Validate refund amount
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (amount > payment.getAmount()) {
            throw new IllegalArgumentException("Refund amount exceeds payment amount");
        }
        
        // Create refund
        Refund refund = new Refund();
        refund.setMerchant(merchant);
        refund.setPayment(payment);
        refund.setAmount(amount);
        refund.setCurrency(payment.getCurrency());
        refund.setStatus("pending");
        refund.setNotes(notes);
        
        Refund savedRefund = refundRepository.save(refund);
        
        // Enqueue refund processing job
        ProcessRefundJob job = new ProcessRefundJob();
        job.setRefundId(savedRefund.getId());
        job.setPaymentId(paymentId);
        job.setMerchantId(merchant.getId());
        
        jobService.enqueueJob(job);
        
        return savedRefund;
    }

    public void updateRefundStatus(String refundId, String status) {
        Optional<Refund> refundOpt = refundRepository.findById(refundId);
        if (refundOpt.isEmpty()) {
            throw new IllegalArgumentException("Refund not found");
        }
        
        Refund refund = refundOpt.get();
        refund.setStatus(status);
        
        if ("completed".equals(status)) {
            refund.setCompletedAt(java.time.OffsetDateTime.now());
        }
        
        refundRepository.save(refund);
    }

    public Refund getRefund(String refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));
    }

    public long calculateAvailableRefundAmount(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Payment not found");
        }
        
        Payment payment = paymentOpt.get();
        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);
        
        long refundedAmount = refunds.stream()
                .map(Refund::getAmount)
                .reduce(0L, Long::sum);
        
        return payment.getAmount() - refundedAmount;
    }
}
