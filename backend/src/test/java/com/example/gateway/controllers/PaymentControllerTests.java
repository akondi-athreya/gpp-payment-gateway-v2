package com.example.gateway.controllers;

import com.example.gateway.dto.CreatePaymentRequest;
import com.example.gateway.models.Payment;
import com.example.gateway.models.Merchant;
import com.example.gateway.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("Payment Controller Tests")
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant testMerchant;
    private Payment testPayment;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("test@example.com");

        testPayment = new Payment();
        testPayment.setId("pay_test_123");
        testPayment.setMerchant(testMerchant);
        testPayment.setAmount(50000);
        testPayment.setCurrency("INR");
        testPayment.setStatus("pending");
        testPayment.setMethod("upi");
    }

    @Test
    @DisplayName("Should create payment with UPI successfully")
    void testCreatePayment_UPI_Success() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("upi");
        request.setVpa("user@paytm");

        when(paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null))
                .thenReturn(testPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay_test_123"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.method").value("upi"))
                .andExpect(jsonPath("$.status").value("pending"));

        verify(paymentService, times(1)).createPayment(anyString(), anyString(), any(CreatePaymentRequest.class), isNull());
    }

    @Test
    @DisplayName("Should create payment with Card successfully")
    void testCreatePayment_Card_Success() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("card");
        
        CreatePaymentRequest.CardRequest card = new CreatePaymentRequest.CardRequest();
        card.setNumber("4111111111111111");
        card.setExpiryMonth(12);
        card.setExpiryYear(25);
        card.setCvv("123");
        card.setName("John Doe");
        request.setCard(card);

        testPayment.setMethod("card");

        when(paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null))
                .thenReturn(testPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("card"));

        verify(paymentService, times(1)).createPayment(anyString(), anyString(), any(CreatePaymentRequest.class), isNull());
    }

    @Test
    @DisplayName("Should handle idempotency key in payment creation")
    void testCreatePayment_WithIdempotencyKey() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("upi");
        request.setVpa("user@paytm");

        when(paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, "idempotent_key_123"))
                .thenReturn(testPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .header("Idempotency-Key", "idempotent_key_123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay_test_123"));

        verify(paymentService, times(1)).createPayment(anyString(), anyString(), any(CreatePaymentRequest.class), eq("idempotent_key_123"));
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void testCreatePayment_InvalidCredentials() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("upi");

        when(paymentService.createPayment("invalid_key", "invalid_secret", request, null))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "invalid_key")
                .header("X-API-Secret", "invalid_secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 for non-existent order")
    void testCreatePayment_OrderNotFound() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("invalid_order_id");
        request.setMethod("upi");

        when(paymentService.createPayment("key_test_abc123", "secret_test_xyz789", request, null))
                .thenThrow(new IllegalArgumentException("Order not found"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get payment by ID")
    void testGetPayment() throws Exception {
        // Arrange
        when(paymentService.getPayment("key_test_abc123", "secret_test_xyz789", "pay_test_123"))
                .thenReturn(testPayment);

        // Act & Assert
        mockMvc.perform(get("/api/payments/pay_test_123")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay_test_123"))
                .andExpect(jsonPath("$.amount").value(50000));

        verify(paymentService, times(1)).getPayment(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 404 for non-existent payment")
    void testGetPayment_NotFound() throws Exception {
        // Arrange
        when(paymentService.getPayment("key_test_abc123", "secret_test_xyz789", "invalid_payment_id"))
                .thenThrow(new IllegalArgumentException("Payment not found"));

        // Act & Assert
        mockMvc.perform(get("/api/payments/invalid_payment_id")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should validate payment method")
    void testCreatePayment_InvalidMethod() throws Exception {
        // Arrange
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order_test_123");
        request.setMethod("invalid_method");

        when(paymentService.createPayment(anyString(), anyString(), any(CreatePaymentRequest.class), isNull()))
                .thenThrow(new IllegalArgumentException("Invalid payment method"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
