package com.example.gateway;

import com.example.gateway.dto.CreateOrderRequest;
import com.example.gateway.dto.CreatePaymentRequest;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.Order;
import com.example.gateway.models.Payment;
import com.example.gateway.repositories.MerchantRepository;
import com.example.gateway.repositories.OrderRepository;
import com.example.gateway.repositories.PaymentRepository;
import com.example.gateway.services.OrderService;
import com.example.gateway.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Payment Flow Integration Tests")
class PaymentFlowIntegrationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Merchant testMerchant;

    @BeforeEach
    void setUp() {
        testMerchant = new Merchant();
        testMerchant.setId(UUID.randomUUID());
        testMerchant.setEmail("integration@example.com");
        testMerchant.setApiKey("test_key_integration");
        testMerchant.setApiSecret("test_secret_integration");
        testMerchant.setWebhookUrl("https://example.com/webhooks");
        testMerchant.setWebhookSecret("webhook_secret");
        
        testMerchant = merchantRepository.save(testMerchant);
    }

    @Test
    @DisplayName("Should complete full payment flow: create order -> create payment")
    void testCompletePaymentFlow_UPI() {
        // Step 1: Create Order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setAmount(50000);
        orderRequest.setCurrency("INR");
        orderRequest.setNotes("Integration test order");

        Order createdOrder = orderService.createOrder(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                orderRequest
        );

        assertNotNull(createdOrder);
        assertEquals(50000, createdOrder.getAmount());
        assertEquals("INR", createdOrder.getCurrency());
        assertEquals("created", createdOrder.getStatus());

        // Verify order in database
        Order savedOrder = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertEquals("created", savedOrder.getStatus());

        // Step 2: Create Payment for the Order
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(createdOrder.getId());
        paymentRequest.setMethod("upi");
        paymentRequest.setVpa("user@paytm");

        Payment createdPayment = paymentService.createPayment(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                paymentRequest,
                null
        );

        assertNotNull(createdPayment);
        assertEquals(createdOrder.getId(), createdPayment.getOrder().getId());
        assertEquals("upi", createdPayment.getMethod());
        assertEquals("user@paytm", createdPayment.getVpa());
        assertEquals("pending", createdPayment.getStatus());

        // Verify payment in database
        Payment savedPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
        assertEquals("pending", savedPayment.getStatus());
        assertEquals(testMerchant.getId(), savedPayment.getMerchant().getId());
    }

    @Test
    @DisplayName("Should complete full payment flow with Card payment")
    void testCompletePaymentFlow_Card() {
        // Step 1: Create Order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setAmount(100000);
        orderRequest.setCurrency("INR");
        orderRequest.setNotes("Card payment test");

        Order createdOrder = orderService.createOrder(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                orderRequest
        );

        assertNotNull(createdOrder);
        assertTrue(createdOrder.getId().startsWith("order_"));

        // Step 2: Create Card Payment
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(createdOrder.getId());
        paymentRequest.setMethod("card");

        CreatePaymentRequest.CardRequest card = new CreatePaymentRequest.CardRequest();
        card.setNumber("4111111111111111");
        card.setExpiryMonth(12);
        card.setExpiryYear(25);
        card.setCvv("123");
        card.setName("John Doe");
        paymentRequest.setCard(card);

        Payment createdPayment = paymentService.createPayment(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                paymentRequest,
                null
        );

        assertNotNull(createdPayment);
        assertEquals("card", createdPayment.getMethod());
        assertEquals("pending", createdPayment.getStatus());
    }

    @Test
    @DisplayName("Should handle idempotency key in payment creation")
    void testPaymentFlow_WithIdempotencyKey() {
        // Create order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setAmount(50000);
        orderRequest.setCurrency("INR");

        Order createdOrder = orderService.createOrder(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                orderRequest
        );

        // Create payment with idempotency key
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(createdOrder.getId());
        paymentRequest.setMethod("upi");
        paymentRequest.setVpa("user@paytm");

        String idempotencyKey = "idempotent_" + UUID.randomUUID();

        Payment payment1 = paymentService.createPayment(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                paymentRequest,
                idempotencyKey
        );

        assertNotNull(payment1);

        // Attempt to create same payment with same idempotency key
        Payment payment2 = paymentService.createPayment(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                paymentRequest,
                idempotencyKey
        );

        assertNotNull(payment2);
        assertEquals(payment1.getId(), payment2.getId());
    }

    @Test
    @DisplayName("Should reject payment with invalid order")
    void testPaymentFlow_InvalidOrder() {
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId("invalid_order_id");
        paymentRequest.setMethod("upi");
        paymentRequest.setVpa("user@paytm");

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(
                        testMerchant.getApiKey(),
                        testMerchant.getApiSecret(),
                        paymentRequest,
                        null
                )
        );
    }

    @Test
    @DisplayName("Should reject payment with invalid credentials")
    void testPaymentFlow_InvalidCredentials() {
        // Create order first
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setAmount(50000);
        orderRequest.setCurrency("INR");

        Order createdOrder = orderService.createOrder(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                orderRequest
        );

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(createdOrder.getId());
        paymentRequest.setMethod("upi");
        paymentRequest.setVpa("user@paytm");

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.createPayment(
                        "invalid_key",
                        "invalid_secret",
                        paymentRequest,
                        null
                )
        );
    }

    @Test
    @DisplayName("Should track payment status changes")
    void testPaymentFlow_StatusTracking() {
        // Create order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setAmount(50000);
        orderRequest.setCurrency("INR");

        Order createdOrder = orderService.createOrder(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                orderRequest
        );

        // Create payment
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(createdOrder.getId());
        paymentRequest.setMethod("upi");
        paymentRequest.setVpa("user@paytm");

        Payment createdPayment = paymentService.createPayment(
                testMerchant.getApiKey(),
                testMerchant.getApiSecret(),
                paymentRequest,
                null
        );

        assertEquals("pending", createdPayment.getStatus());

        // Simulate payment success
        paymentService.updatePaymentStatus(createdPayment.getId(), "success");
        Payment successPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
        assertEquals("success", successPayment.getStatus());

        // Simulate payment failure
        paymentService.updatePaymentStatus(createdPayment.getId(), "failed");
        Payment failedPayment = paymentRepository.findById(createdPayment.getId()).orElseThrow();
        assertEquals("failed", failedPayment.getStatus());
    }
}
