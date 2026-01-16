package com.example.gateway.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class WebhookPayloadBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookPayloadBuilder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Build webhook payload for payment event
     */
    public JsonNode buildPaymentPayload(String event, Payment payment) {
        ObjectNode root = objectMapper.createObjectNode();
        
        root.put("event", event);
        root.put("timestamp", Instant.now().getEpochSecond());
        
        ObjectNode data = objectMapper.createObjectNode();
        ObjectNode paymentNode = buildPaymentNode(payment);
        data.set("payment", paymentNode);
        
        root.set("data", data);
        
        logger.debug("Built webhook payload for event: {}, payment: {}", event, payment.getId());
        return root;
    }
    
    /**
     * Build webhook payload for refund event
     */
    public JsonNode buildRefundPayload(String event, Refund refund) {
        ObjectNode root = objectMapper.createObjectNode();
        
        root.put("event", event);
        root.put("timestamp", Instant.now().getEpochSecond());
        
        ObjectNode data = objectMapper.createObjectNode();
        ObjectNode refundNode = buildRefundNode(refund);
        data.set("refund", refundNode);
        
        root.set("data", data);
        
        logger.debug("Built webhook payload for event: {}, refund: {}", event, refund.getId());
        return root;
    }
    
    /**
     * Build payment object node for payload
     */
    private ObjectNode buildPaymentNode(Payment payment) {
        ObjectNode node = objectMapper.createObjectNode();
        
        node.put("id", payment.getId());
        node.put("order_id", payment.getOrder() != null ? payment.getOrder().getId() : null);
        node.put("amount", payment.getAmount());
        node.put("currency", payment.getCurrency());
        node.put("method", payment.getMethod() != null ? payment.getMethod().toString() : null);
        
        if (payment.getVpa() != null) {
            node.put("vpa", payment.getVpa());
        }
        
        if (payment.getCardNetwork() != null) {
            node.put("card_network", payment.getCardNetwork().toString());
        }
        
        if (payment.getCardLast4() != null) {
            node.put("card_last4", payment.getCardLast4());
        }
        
        node.put("status", payment.getStatus());
        node.put("captured", payment.getCaptured());
        
        if (payment.getCreatedAt() != null) {
            node.put("created_at", payment.getCreatedAt().toString());
        }
        
        if (payment.getUpdatedAt() != null) {
            node.put("updated_at", payment.getUpdatedAt().toString());
        }
        
        if (payment.getErrorCode() != null) {
            node.put("error_code", payment.getErrorCode());
        }
        
        if (payment.getErrorDescription() != null) {
            node.put("error_description", payment.getErrorDescription());
        }
        
        return node;
    }
    
    /**
     * Build refund object node for payload
     */
    private ObjectNode buildRefundNode(Refund refund) {
        ObjectNode node = objectMapper.createObjectNode();
        
        node.put("id", refund.getId());
        node.put("payment_id", refund.getPayment() != null ? refund.getPayment().getId() : null);
        node.put("amount", refund.getAmount());
        node.put("status", refund.getStatus());
        
        if (refund.getReason() != null) {
            node.put("reason", refund.getReason());
        }
        
        if (refund.getCreatedAt() != null) {
            node.put("created_at", refund.getCreatedAt().toString());
        }
        
        if (refund.getProcessedAt() != null) {
            node.put("processed_at", refund.getProcessedAt().toString());
        }
        
        return node;
    }
}
