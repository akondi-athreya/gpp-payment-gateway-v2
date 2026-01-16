package com.example.gateway.services;

import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class WebhookServiceImpl implements WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookServiceImpl.class);
    
    @Override
    public void enqueueWebhookDelivery(UUID merchantId, String event, Payment payment, Refund refund) {
        // Temporary stub - Full implementation in Phase 3
        logger.info("Webhook delivery enqueued for merchant: {}, event: {}", merchantId, event);
    }
}
