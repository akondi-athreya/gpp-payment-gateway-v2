package com.example.gateway.services;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class IDGeneratorService {
    
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Generate payment ID: "pay_" + 16 alphanumeric characters
     */
    public String generatePaymentId() {
        return "pay_" + generateRandomString(16);
    }
    /**
     * Generate refund ID: "rfnd_" + 16 alphanumeric characters
     */
    public String generateRefundId() {
        return "rfnd_" + generateRandomString(16);
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
