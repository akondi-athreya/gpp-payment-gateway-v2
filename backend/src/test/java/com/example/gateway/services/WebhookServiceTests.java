package com.example.gateway.services;

import com.example.gateway.models.*;
import com.example.gateway.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Webhook Service Tests")
class WebhookServiceTests {

    @Mock
    private WebhookLogRepository webhookLogRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private WebhookService webhookService;

    private Merchant testMerchant;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("merchant@example.com");
        testMerchant.setWebhookUrl("https://merchant.example.com/webhooks");
        testMerchant.setWebhookSecret("webhook_secret_123");
    }

    @Test
    @DisplayName("Should enqueue webhook delivery successfully")
    void testEnqueueWebhook_Success() {
        // Arrange
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setId("webhook_123");
        webhookLog.setMerchant(testMerchant);
        webhookLog.setEventType("payment.created");
        webhookLog.setPayload("{\"id\":\"pay_123\",\"status\":\"pending\"}");
        webhookLog.setStatus("pending");
        webhookLog.setRetryCount(0);

        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhook(testMerchant, "payment.created", 
                "{\"id\":\"pay_123\",\"status\":\"pending\"}");

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should handle webhook delivery with exponential backoff")
    void testScheduleRetry_ExponentialBackoff() {
        // Arrange
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setId("webhook_123");
        webhookLog.setMerchant(testMerchant);
        webhookLog.setStatus("failed");
        webhookLog.setRetryCount(2);

        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.scheduleRetry(webhookLog);

        // Assert
        assertEquals(3, webhookLog.getRetryCount());
        assertEquals("failed", webhookLog.getStatus());
        verify(webhookLogRepository, times(1)).save(webhookLog);
    }

    @Test
    @DisplayName("Should update webhook status to delivered")
    void testUpdateWebhookStatus_Delivered() {
        // Arrange
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setId("webhook_123");
        webhookLog.setStatus("pending");
        webhookLog.setResponseStatus(200);

        when(webhookLogRepository.findById("webhook_123"))
                .thenReturn(Optional.of(webhookLog));
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.updateWebhookStatus("webhook_123", "delivered", 200);

        // Assert
        assertEquals("delivered", webhookLog.getStatus());
        assertEquals(200, webhookLog.getResponseStatus());
        verify(webhookLogRepository, times(1)).save(webhookLog);
    }

    @Test
    @DisplayName("Should handle different webhook events")
    void testEnqueueWebhook_PaymentSucceeded() {
        // Arrange
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhook(testMerchant, "payment.succeeded", 
                "{\"id\":\"pay_123\",\"status\":\"success\"}");

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should handle refund webhook events")
    void testEnqueueWebhook_RefundCreated() {
        // Arrange
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhook(testMerchant, "refund.created", 
                "{\"id\":\"refund_123\",\"paymentId\":\"pay_123\"}");

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should get webhook by ID")
    void testGetWebhook() {
        // Arrange
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setId("webhook_123");
        webhookLog.setStatus("delivered");

        when(webhookLogRepository.findById("webhook_123"))
                .thenReturn(Optional.of(webhookLog));

        // Act
        WebhookLog result = webhookService.getWebhook("webhook_123");

        // Assert
        assertNotNull(result);
        assertEquals("delivered", result.getStatus());
    }

    @Test
    @DisplayName("Should handle webhook not found")
    void testGetWebhook_NotFound() {
        // Arrange
        when(webhookLogRepository.findById("invalid_webhook_id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                webhookService.getWebhook("invalid_webhook_id"));
    }
}
