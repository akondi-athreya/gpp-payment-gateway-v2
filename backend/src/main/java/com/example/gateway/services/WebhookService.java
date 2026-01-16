package com.example.gateway.services;

import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import java.util.UUID;

/**
 * Service for managing webhook delivery jobs
 * Implementation details in Phase 3
 */
public interface WebhookService {
    
    /**
     * Enqueue a webhook delivery job for a payment event
     * @param merchantId The merchant receiving the webhook
     * @param event The event type (e.g., "payment.success")
     * @param payment The payment object (for payment events)
     * @param refund The refund object (for refund events)
     */
    void enqueueWebhookDelivery(UUID merchantId, String event, Payment payment, Refund refund);

    /**
     * Enqueue a generic webhook with a provided payload.
     * @param merchantId Merchant receiving the webhook
     * @param event Event name
     * @param payload Custom payload
     * @return webhook log id
     */
    String enqueueCustomWebhook(UUID merchantId, String event, com.fasterxml.jackson.databind.JsonNode payload);
}
