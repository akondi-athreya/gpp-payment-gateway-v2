package com.example.gateway.services;

import com.example.gateway.models.*;
import com.example.gateway.repositories.*;
import com.example.gateway.jobs.JobServiceImpl;
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

    @Mock
    private JobServiceImpl jobService;

    @Mock
    private WebhookPayloadBuilder payloadBuilder;

    @InjectMocks
    private WebhookServiceImpl webhookService;

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
        Payment payment = new Payment();
        payment.setId("pay_123");
        payment.setStatus("pending");
        
        when(payloadBuilder.buildPaymentPayload("payment.created", payment))
                .thenReturn(null); // Mock JSON payload
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhookDelivery(testMerchant.getId(), "payment.created", payment, null);

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should handle payment success webhook")
    void testEnqueueWebhook_PaymentSucceeded() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay_123");
        payment.setStatus("success");
        
        when(payloadBuilder.buildPaymentPayload("payment.success", payment))
                .thenReturn(null);
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhookDelivery(testMerchant.getId(), "payment.success", payment, null);

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }

    @Test
    @DisplayName("Should handle refund webhook events")
    void testEnqueueWebhook_RefundCreated() {
        // Arrange
        Refund refund = new Refund();
        refund.setId("refund_123");
        refund.setStatus("pending");
        
        when(payloadBuilder.buildRefundPayload("refund.created", refund))
                .thenReturn(null);
        when(webhookLogRepository.save(any(WebhookLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        webhookService.enqueueWebhookDelivery(testMerchant.getId(), "refund.created", null, refund);

        // Assert
        verify(webhookLogRepository, times(1)).save(any(WebhookLog.class));
    }
}
