package com.example.gateway.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class WebhookSignatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate HMAC-SHA256 signature for webhook payload
     * @param payload JSON object to sign
     * @param webhookSecret The merchant's webhook secret
     * @return Hex-encoded signature (64 characters for SHA-256)
     */
    public String generateSignature(Object payload, String webhookSecret) {
        try {
            // Convert payload to JSON string (compact, no pretty printing)
            String payloadString;
            if (payload instanceof String) {
                payloadString = (String) payload;
            } else {
                payloadString = objectMapper.writeValueAsString(payload);
            }
            
            // Generate HMAC-SHA256
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(payloadString.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            String signature = bytesToHex(hmacBytes);
            logger.debug("Generated signature for webhook payload with secret length: {}", webhookSecret.length());
            
            return signature;
        } catch (Exception e) {
            logger.error("Error generating webhook signature", e);
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }
    
    /**
     * Verify webhook signature
     * @param payload The webhook payload
     * @param signature The signature to verify
     * @param webhookSecret The merchant's webhook secret
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(Object payload, String signature, String webhookSecret) {
        try {
            String expectedSignature = generateSignature(payload, webhookSecret);
            boolean isValid = constantTimeEquals(expectedSignature, signature);
            
            if (!isValid) {
                logger.warn("Webhook signature verification failed. Expected: {}, Got: {}", 
                        expectedSignature.substring(0, 8) + "***", signature.substring(0, 8) + "***");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Constant time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
    
    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
