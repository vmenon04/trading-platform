package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountSubscriptionDTO Tests")
class AccountSubscriptionDTOTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create AccountSubscriptionDTO with valid data")
    void testValidAccountSubscriptionDTO() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1, 2, "2026-09-25", "ACTIVE");
        
        assertEquals(1, dto.getAccountId());
        assertEquals(2, dto.getModelPortfolioId());
        assertEquals("2026-09-25", dto.getSubscriptionDate());
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    @DisplayName("Should validate positive accountId")
    void testValidateAccountId() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(-1, 2, "2026-09-25", "ACTIVE");
        
        Set<ConstraintViolation<AccountSubscriptionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountId")));
    }

    @Test
    @DisplayName("Should validate positive modelPortfolioId")
    void testValidateModelPortfolioId() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1, -2, "2026-09-25", "ACTIVE");
        
        Set<ConstraintViolation<AccountSubscriptionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("modelPortfolioId")));
    }

    @Test
    @DisplayName("Should validate non-blank subscriptionDate")
    void testValidateBlankSubscriptionDate() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1, 2, "   ", "ACTIVE");
        
        Set<ConstraintViolation<AccountSubscriptionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("subscriptionDate")));
    }

    @Test
    @DisplayName("Should validate non-blank status")
    void testValidateBlankStatus() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1, 2, "2026-09-25", "");
        
        Set<ConstraintViolation<AccountSubscriptionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("Should validate null status")
    void testValidateNullStatus() {
        AccountSubscriptionDTO dto = new AccountSubscriptionDTO(1, 2, "2026-09-25", null);
        
        Set<ConstraintViolation<AccountSubscriptionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        AccountSubscriptionDTO dto1 = new AccountSubscriptionDTO(1, 2, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto2 = new AccountSubscriptionDTO(1, 2, "2026-09-25", "ACTIVE");
        AccountSubscriptionDTO dto3 = new AccountSubscriptionDTO(1, 2, "2026-09-25", "INACTIVE");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}

