package com.example.gateway.controllers;

import com.example.gateway.dto.CreateRefundRequest;
import com.example.gateway.models.Refund;
import com.example.gateway.models.Merchant;
import com.example.gateway.services.RefundService;
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

@WebMvcTest(RefundController.class)
@DisplayName("Refund Controller Tests")
class RefundControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefundService refundService;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant testMerchant;
    private Refund testRefund;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("test@example.com");

        testRefund = new Refund();
        testRefund.setId("refund_test_123");
        testRefund.setMerchant(testMerchant);
        testRefund.setAmount(25000);
        testRefund.setCurrency("INR");
        testRefund.setStatus("pending");
    }

    @Test
    @DisplayName("Should create full refund successfully")
    void testCreateRefund_FullRefund_Success() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(50000);

        when(refundService.createRefund("key_test_abc123", "secret_test_xyz789", 
                "pay_test_123", 50000, null))
                .thenReturn(testRefund);

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("refund_test_123"))
                .andExpect(jsonPath("$.amount").value(25000))
                .andExpect(jsonPath("$.status").value("pending"));

        verify(refundService, times(1)).createRefund(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should create partial refund successfully")
    void testCreateRefund_PartialRefund_Success() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(25000);

        when(refundService.createRefund("key_test_abc123", "secret_test_xyz789", 
                "pay_test_123", 25000, null))
                .thenReturn(testRefund);

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("refund_test_123"));

        verify(refundService, times(1)).createRefund(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 when refund exceeds payment amount")
    void testCreateRefund_ExceedsPaymentAmount() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(60000);

        when(refundService.createRefund(anyString(), anyString(), anyString(), eq(60000L), any()))
                .thenThrow(new IllegalArgumentException("Refund amount exceeds payment amount"));

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 for non-existent payment")
    void testCreateRefund_PaymentNotFound() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("invalid_payment_id");
        request.setAmount(25000);

        when(refundService.createRefund("key_test_abc123", "secret_test_xyz789", 
                "invalid_payment_id", 25000, null))
                .thenThrow(new IllegalArgumentException("Payment not found"));

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void testCreateRefund_InvalidCredentials() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(25000);

        when(refundService.createRefund("invalid_key", "invalid_secret", 
                "pay_test_123", 25000, null))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "invalid_key")
                .header("X-API-Secret", "invalid_secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get refund by ID")
    void testGetRefund() throws Exception {
        // Arrange
        when(refundService.getRefund("refund_test_123"))
                .thenReturn(testRefund);

        // Act & Assert
        mockMvc.perform(get("/api/refunds/refund_test_123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("refund_test_123"))
                .andExpect(jsonPath("$.amount").value(25000));

        verify(refundService, times(1)).getRefund(anyString());
    }

    @Test
    @DisplayName("Should return 404 for non-existent refund")
    void testGetRefund_NotFound() throws Exception {
        // Arrange
        when(refundService.getRefund("invalid_refund_id"))
                .thenThrow(new IllegalArgumentException("Refund not found"));

        // Act & Assert
        mockMvc.perform(get("/api/refunds/invalid_refund_id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject zero or negative refund amount")
    void testCreateRefund_InvalidAmount() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(0);

        when(refundService.createRefund(anyString(), anyString(), anyString(), eq(0L), any()))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include notes in refund request")
    void testCreateRefund_WithNotes() throws Exception {
        // Arrange
        CreateRefundRequest request = new CreateRefundRequest();
        request.setPaymentId("pay_test_123");
        request.setAmount(25000);
        request.setNotes("Refund due to user request");

        when(refundService.createRefund("key_test_abc123", "secret_test_xyz789", 
                "pay_test_123", 25000, "Refund due to user request"))
                .thenReturn(testRefund);

        // Act & Assert
        mockMvc.perform(post("/api/refunds")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(refundService, times(1)).createRefund(anyString(), anyString(), anyString(), anyLong(), eq("Refund due to user request"));
    }
}
