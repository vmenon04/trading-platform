package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountHoldingDTO Tests")
class AccountHoldingDTOTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Should create AccountHoldingDTO with valid data")
    void testValidAccountHoldingDTO() {
        AccountHoldingDTO dto = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(100), "ACTIVE");
        
        assertEquals(1, dto.accountId());
        assertEquals(2, dto.instrumentId());
        assertEquals("2026-09-25", dto.asOfDate());
        assertEquals(BigDecimal.valueOf(100), dto.quantity());
        assertEquals("ACTIVE", dto.status());
    }

    @Test
    @DisplayName("Should validate positive accountId")
    void testValidateAccountId() {
        AccountHoldingDTO dto = new AccountHoldingDTO(invalidAccountId(), 2, "2026-09-25", BigDecimal.valueOf(100), "ACTIVE");
        
        Set<ConstraintViolation<AccountHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountId")));
    }

    @Test
    @DisplayName("Should validate positive instrumentId")
    void testValidateInstrumentId() {
        AccountHoldingDTO dto = new AccountHoldingDTO(1, invalidInstrumentId(), "2026-09-25", BigDecimal.valueOf(100), "ACTIVE");
        
        Set<ConstraintViolation<AccountHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("instrumentId")));
    }

    @Test
    @DisplayName("Should validate non-blank asOfDate")
    void testValidateBlankAsOfDate() {
        AccountHoldingDTO dto = new AccountHoldingDTO(1, 2, "   ", BigDecimal.valueOf(100), "ACTIVE");
        
        Set<ConstraintViolation<AccountHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("asOfDate")));
    }

    @Test
    @DisplayName("Should validate positive quantity")
    void testValidateQuantity() {
        AccountHoldingDTO dto = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(-100), "ACTIVE");
        
        Set<ConstraintViolation<AccountHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    @DisplayName("Should validate non-blank status")
    void testValidateBlankStatus() {
        AccountHoldingDTO dto = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(100), "");
        
        Set<ConstraintViolation<AccountHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        AccountHoldingDTO dto1 = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(100), "ACTIVE");
        AccountHoldingDTO dto2 = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(100), "ACTIVE");
        AccountHoldingDTO dto3 = new AccountHoldingDTO(1, 2, "2026-09-25", BigDecimal.valueOf(100), "INACTIVE");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    private int invalidAccountId() {
        return Integer.parseInt("-1");
    }

    private int invalidInstrumentId() {
        return Integer.parseInt("-2");
    }
}

