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
@DisplayName("Refund Service Tests")
class RefundServiceTests {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private RefundService refundService;

    private Merchant testMerchant;
    private Payment testPayment;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("merchant@example.com");

        testPayment = new Payment();
        testPayment.setId("pay_test_123");
        testPayment.setMerchant(testMerchant);
        testPayment.setAmount(50000);
        testPayment.setCurrency("INR");
        testPayment.setStatus("success");
        testPayment.setMethod("upi");
    }

    @Test
    @DisplayName("Should create full refund successfully")
    void testCreateRefund_FullRefund_Success() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(testPayment));
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> {
                    Refund refund = invocation.getArgument(0);
                    refund.setId("refund_test_123");
                    return refund;
                });

        // Act
        Refund result = refundService.createRefund(
                "key_test_abc123", "secret_test_xyz789",
                "pay_test_123", 50000, "Full refund");

        // Assert
        assertNotNull(result);
        assertEquals(50000, result.getAmount());
        assertEquals("pending", result.getStatus());
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    @DisplayName("Should create partial refund successfully")
    void testCreateRefund_PartialRefund_Success() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(testPayment));
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Refund result = refundService.createRefund(
                "key_test_abc123", "secret_test_xyz789",
                "pay_test_123", 25000, "Partial refund");

        // Assert
        assertNotNull(result);
        assertEquals(25000, result.getAmount());
        assertEquals("pending", result.getStatus());
    }

    @Test
    @DisplayName("Should reject refund exceeding payment amount")
    void testCreateRefund_ExceedsPaymentAmount() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                refundService.createRefund(
                        "key_test_abc123", "secret_test_xyz789",
                        "pay_test_123", 60000, "Exceeds payment"));
    }

    @Test
    @DisplayName("Should reject refund for non-successful payment")
    void testCreateRefund_InvalidPaymentStatus() {
        // Arrange
        testPayment.setStatus("failed");
        
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                refundService.createRefund(
                        "key_test_abc123", "secret_test_xyz789",
                        "pay_test_123", 25000, "Invalid status"));
    }

    @Test
    @DisplayName("Should reject refund with invalid credentials")
    void testCreateRefund_InvalidCredentials() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                refundService.createRefund(
                        "invalid_key", "invalid_secret",
                        "pay_test_123", 25000, "Invalid creds"));
    }

    @Test
    @DisplayName("Should reject refund for non-existent payment")
    void testCreateRefund_PaymentNotFound() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("invalid_payment_id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                refundService.createRefund(
                        "key_test_abc123", "secret_test_xyz789",
                        "invalid_payment_id", 25000, "Not found"));
    }

    @Test
    @DisplayName("Should reject zero or negative refund amount")
    void testCreateRefund_InvalidAmount() {
        // Arrange
        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                refundService.createRefund(
                        "key_test_abc123", "secret_test_xyz789",
                        "pay_test_123", 0, "Invalid amount"));
    }

    @Test
    @DisplayName("Should update refund status to completed")
    void testUpdateRefundStatus_Completed() {
        // Arrange
        Refund refund = new Refund();
        refund.setId("refund_test_123");
        refund.setStatus("pending");

        when(refundRepository.findById("refund_test_123"))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(Refund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refundService.updateRefundStatus("refund_test_123", "completed");

        // Assert
        assertEquals("completed", refund.getStatus());
        verify(refundRepository, times(1)).save(refund);
    }

    @Test
    @DisplayName("Should get refund by ID")
    void testGetRefund() {
        // Arrange
        Refund refund = new Refund();
        refund.setId("refund_test_123");
        refund.setAmount(25000);
        refund.setStatus("completed");

        when(refundRepository.findById("refund_test_123"))
                .thenReturn(Optional.of(refund));

        // Act
        Refund result = refundService.getRefund("refund_test_123");

        // Assert
        assertNotNull(result);
        assertEquals("refund_test_123", result.getId());
        assertEquals(25000, result.getAmount());
    }
}
