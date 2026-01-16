package com.example.gateway.services;

import com.example.gateway.dto.CreatePaymentRequest;
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
@DisplayName("Payment Service Tests")
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Merchant testMerchant;
    private Order testOrder;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("test@example.com");
        testMerchant.setApiKey("key_test_abc123");
        testMerchant.setApiSecret("secret_test_xyz789");

        testOrder = new Order();
        testOrder.setId("order_test_123");
        testOrder.setMerchant(testMerchant);
        testOrder.setAmount(50000);
        testOrder.setCurrency("INR");
        testOrder.setStatus("created");
    }

    @Test
    @DisplayName("Should create payment successfully with UPI method")
    void testCreatePayment_UPI_Success() {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("upi");
        request.setVpa("user@paytm");

        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(orderRepository.findById("order_test_123"))
                .thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId("pay_test_123");
                    return payment;
                });

        // Act
        Payment result = paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null);

        // Assert
        assertNotNull(result);
        assertEquals("upi", result.getMethod());
        assertEquals("user@paytm", result.getVpa());
        assertEquals("pending", result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid order")
    void testCreatePayment_InvalidOrder() {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("invalid_order_id");
        request.setMethod("upi");

        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(orderRepository.findById("invalid_order_id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null));
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void testCreatePayment_InvalidCredentials() {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("upi");

        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment("invalid_key", "invalid_secret", request, null));
    }

    @Test
    @DisplayName("Should validate payment method")
    void testCreatePayment_InvalidMethod() {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("invalid_method");

        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.of(testMerchant));
        when(orderRepository.findById("order_test_123"))
                .thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null));
    }

    @Test
    @DisplayName("Should update payment status correctly")
    void testUpdatePaymentStatus() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay_test_123");
        payment.setStatus("pending");

        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        paymentService.updatePaymentStatus("pay_test_123", "success");

        // Assert
        assertEquals("success", payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    @DisplayName("Should get payment by ID")
    void testGetPayment() {
        // Arrange
        Payment payment = new Payment();
        payment.setId("pay_test_123");
        payment.setAmount(50000);
        payment.setMerchant(testMerchant);

        when(paymentRepository.findById("pay_test_123"))
                .thenReturn(Optional.of(payment));

        // Act
        Payment result = paymentService.getPayment("key_test_abc123", "secret_test_xyz789", "pay_test_123");

        // Assert
        assertNotNull(result);
        assertEquals(50000, result.getAmount());
    }
}
