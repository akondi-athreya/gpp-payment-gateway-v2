package com.example.gateway.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Validation Service Tests")
class ValidationServiceTests {

    @InjectMocks
    private ValidationService validationService;

    @Test
    @DisplayName("Should validate positive amount")
    void testValidateAmount_Positive() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> validationService.validateAmount(50000));
    }

    @Test
    @DisplayName("Should reject zero amount")
    void testValidateAmount_Zero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateAmount(0));
    }

    @Test
    @DisplayName("Should reject negative amount")
    void testValidateAmount_Negative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateAmount(-1000));
    }

    @Test
    @DisplayName("Should validate valid currency codes")
    void testValidateCurrency_Valid() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> validationService.validateCurrency("INR"));
        assertDoesNotThrow(() -> validationService.validateCurrency("USD"));
        assertDoesNotThrow(() -> validationService.validateCurrency("EUR"));
    }

    @Test
    @DisplayName("Should reject invalid currency code")
    void testValidateCurrency_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateCurrency("INVALID"));
    }

    @Test
    @DisplayName("Should validate valid payment methods")
    void testValidatePaymentMethod_Valid() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> validationService.validatePaymentMethod("upi"));
        assertDoesNotThrow(() -> validationService.validatePaymentMethod("card"));
        assertDoesNotThrow(() -> validationService.validatePaymentMethod("netbanking"));
    }

    @Test
    @DisplayName("Should reject invalid payment method")
    void testValidatePaymentMethod_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validatePaymentMethod("invalid_method"));
    }

    @Test
    @DisplayName("Should validate UPI VPA format")
    void testValidateVPA_Valid() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> validationService.validateVPA("user@paytm"));
        assertDoesNotThrow(() -> validationService.validateVPA("john@okhdfcbank"));
    }

    @Test
    @DisplayName("Should reject invalid UPI VPA format")
    void testValidateVPA_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateVPA("invalid-vpa"));
    }

    @Test
    @DisplayName("Should validate email format")
    void testValidateEmail_Valid() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> validationService.validateEmail("test@example.com"));
        assertDoesNotThrow(() -> validationService.validateEmail("user.name@domain.co.uk"));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void testValidateEmail_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateEmail("invalid-email"));
    }

    @Test
    @DisplayName("Should validate card number - valid Visa")
    void testValidateCardNumber_ValidVisa() {
        // Visa number (4xxx xxxx xxxx xxxx)
        assertDoesNotThrow(() -> 
                validationService.validateCardNumber("4111111111111111"));
    }

    @Test
    @DisplayName("Should reject invalid card number - too short")
    void testValidateCardNumber_TooShort() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateCardNumber("4111111"));
    }

    @Test
    @DisplayName("Should validate card expiry - valid")
    void testValidateCardExpiry_Valid() {
        // Act & Assert - card expiring in future
        assertDoesNotThrow(() -> 
                validationService.validateCardExpiry(12, 25));
    }

    @Test
    @DisplayName("Should reject card expiry - invalid month")
    void testValidateCardExpiry_InvalidMonth() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateCardExpiry(13, 25));
    }

    @Test
    @DisplayName("Should validate CVV format")
    void testValidateCVV_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateCVV("123"));
        assertDoesNotThrow(() -> validationService.validateCVV("1234"));
    }

    @Test
    @DisplayName("Should reject invalid CVV format")
    void testValidateCVV_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateCVV("12"));
    }

    @Test
    @DisplayName("Should validate API key format")
    void testValidateApiKey_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateApiKey("key_test_abc123"));
    }

    @Test
    @DisplayName("Should reject invalid API key format")
    void testValidateApiKey_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateApiKey("short"));
    }

    @Test
    @DisplayName("Should validate webhook URL format")
    void testValidateWebhookUrl_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateWebhookUrl("https://example.com/webhooks"));
    }

    @Test
    @DisplayName("Should reject invalid webhook URL format")
    void testValidateWebhookUrl_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateWebhookUrl("not-a-url"));
    }

    @Test
    @DisplayName("Should validate idempotency key format")
    void testValidateIdempotencyKey_Valid() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateIdempotencyKey("idempotent_key_123"));
    }

    @Test
    @DisplayName("Should reject invalid idempotency key format")
    void testValidateIdempotencyKey_Invalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
                validationService.validateIdempotencyKey(""));
    }
}
