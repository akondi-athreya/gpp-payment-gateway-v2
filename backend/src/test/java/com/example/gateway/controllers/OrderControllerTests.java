package com.example.gateway.controllers;

import com.example.gateway.dto.CreateOrderRequest;
import com.example.gateway.models.Order;
import com.example.gateway.models.Merchant;
import com.example.gateway.services.OrderService;
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

@WebMvcTest(OrderController.class)
@DisplayName("Order Controller Tests")
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant testMerchant;
    private Order testOrder;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("test@example.com");

        testOrder = new Order();
        testOrder.setId("order_test_123");
        testOrder.setMerchant(testMerchant);
        testOrder.setAmount(50000);
        testOrder.setCurrency("INR");
        testOrder.setStatus("created");
    }

    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrder_Success() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");
        request.setNotes("Test order");

        when(orderService.createOrder("key_test_abc123", "secret_test_xyz789", request))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order_test_123"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.status").value("created"));

        verify(orderService, times(1)).createOrder(anyString(), anyString(), any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid amount")
    void testCreateOrder_InvalidAmount() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(-1000);
        request.setCurrency("INR");

        when(orderService.createOrder(anyString(), anyString(), any(CreateOrderRequest.class)))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void testCreateOrder_InvalidCredentials() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");

        when(orderService.createOrder("invalid_key", "invalid_secret", request))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("X-API-Key", "invalid_key")
                .header("X-API-Secret", "invalid_secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get order by ID")
    void testGetOrder() throws Exception {
        // Arrange
        when(orderService.getOrder("key_test_abc123", "secret_test_xyz789", "order_test_123"))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(get("/api/orders/order_test_123")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order_test_123"))
                .andExpect(jsonPath("$.amount").value(50000));

        verify(orderService, times(1)).getOrder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return 404 for non-existent order")
    void testGetOrder_NotFound() throws Exception {
        // Arrange
        when(orderService.getOrder("key_test_abc123", "secret_test_xyz789", "invalid_order_id"))
                .thenThrow(new IllegalArgumentException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/invalid_order_id")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should validate currency format")
    void testCreateOrder_InvalidCurrency() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INVALID");

        when(orderService.createOrder(anyString(), anyString(), any(CreateOrderRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid currency"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should create order with notes")
    void testCreateOrder_WithNotes() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");
        request.setNotes("Customer order for subscription");

        when(orderService.createOrder("key_test_abc123", "secret_test_xyz789", request))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .header("X-API-Key", "key_test_abc123")
                .header("X-API-Secret", "secret_test_xyz789")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order_test_123"));

        verify(orderService, times(1)).createOrder(anyString(), anyString(), any(CreateOrderRequest.class));
    }
}
