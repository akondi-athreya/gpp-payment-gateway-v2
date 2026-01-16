package com.example.gateway.services;

import com.example.gateway.repositories.PaymentRepository;
import com.example.gateway.repositories.RefundRepository;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class IDGeneratorService {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public IDGeneratorService(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }
    
    /**
     * Generate payment ID: "pay_" + 16 alphanumeric characters
     */
    public String generatePaymentId() {
        String candidate;
        do {
            candidate = "pay_" + generateRandomString(16);
        } while (paymentRepository.findById(candidate).isPresent());
        return candidate;
    }
    /**
     * Generate refund ID: "rfnd_" + 16 alphanumeric characters
     */
    public String generateRefundId() {
        String candidate;
        do {
            candidate = "rfnd_" + generateRandomString(16);
        } while (refundRepository.findById(candidate).isPresent());
        return candidate;
    }
    
    /**
     * Generate webhook job ID: "wh_" + 16 alphanumeric characters
     */
    public String generateWebhookJobId() {
        return "wh_" + generateRandomString(16);
    }
    
    /**
     * Generate random alphanumeric string of specified length
     */
    public String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
