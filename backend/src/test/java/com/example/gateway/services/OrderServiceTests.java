package com.example.gateway.services;

import com.example.gateway.dto.CreateOrderRequest;
import com.example.gateway.models.Order;
import com.example.gateway.models.Merchant;
import com.example.gateway.repositories.OrderRepository;
import com.example.gateway.repositories.MerchantRepository;
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
@DisplayName("Order Service Tests")
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private OrderService orderService;

    private Merchant testMerchant;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        
        testMerchant = new Merchant();
        testMerchant.setId(merchantId);
        testMerchant.setEmail("test@example.com");
        testMerchant.setApiKey("key_test_abc123");
        testMerchant.setApiSecret("secret_test_xyz789");
    }

    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrder_Success() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");
        request.setNotes("Test order");

        when(merchantRepository.findByApiKeyAndApiSecret("key_test_abc123", "secret_test_xyz789"))
                .thenReturn(Optional.of(testMerchant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId("order_test_123");
                    return order;
                });

        // Act
        Order result = orderService.createOrder("key_test_abc123", "secret_test_xyz789", request);

        // Assert
        assertNotNull(result);
        assertEquals(50000, result.getAmount());
        assertEquals("INR", result.getCurrency());
        assertEquals("created", result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject order with invalid credentials")
    void testCreateOrder_InvalidCredentials() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");

        when(merchantRepository.findByApiKeyAndApiSecret("invalid_key", "invalid_secret"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder("invalid_key", "invalid_secret", request));
    }

    @Test
    @DisplayName("Should reject order with invalid amount")
    void testCreateOrder_InvalidAmount() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(-1000); // Negative amount
        request.setCurrency("INR");

        when(merchantRepository.findByApiKeyAndApiSecret("key_test_abc123", "secret_test_xyz789"))
                .thenReturn(Optional.of(testMerchant));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder("key_test_abc123", "secret_test_xyz789", request));
    }

    @Test
    @DisplayName("Should reject order with invalid currency")
    void testCreateOrder_InvalidCurrency() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INVALID");

        when(merchantRepository.findByApiKeyAndApiSecret("key_test_abc123", "secret_test_xyz789"))
                .thenReturn(Optional.of(testMerchant));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder("key_test_abc123", "secret_test_xyz789", request));
    }

    @Test
    @DisplayName("Should create order with notes")
    void testCreateOrder_WithNotes() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");
        request.setNotes("Customer notes here");

        when(merchantRepository.findByApiKeyAndApiSecret("key_test_abc123", "secret_test_xyz789"))
                .thenReturn(Optional.of(testMerchant));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createOrder("key_test_abc123", "secret_test_xyz789", request);

        // Assert
        assertNotNull(result);
        assertEquals("Customer notes here", result.getNotes());
    }

    @Test
    @DisplayName("Should throw exception for merchant not found")
    void testCreateOrder_MerchantNotFound() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAmount(50000);
        request.setCurrency("INR");

        when(merchantRepository.findByApiKeyAndApiSecret(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                orderService.createOrder("key", "secret", request));
    }

    @Test
    @DisplayName("Should get order by ID")
    void testGetOrder() {
        // Arrange
        Order order = new Order();
        order.setId("order_test_123");
        order.setAmount(50000);
        order.setCurrency("INR");
        order.setStatus("created");
        order.setMerchant(testMerchant);

        when(orderRepository.findById("order_test_123"))
                .thenReturn(Optional.of(order));

        // Act
        Order result = orderService.getOrder("key_test_abc123", "secret_test_xyz789", "order_test_123");

        // Assert
        assertNotNull(result);
        assertEquals("order_test_123", result.getId());
    }
}
