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

@DisplayName("AccountTradeDTO Tests")
class AccountTradeDTOTest {
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
    @DisplayName("Should create AccountTradeDTO with valid data")
    void testValidAccountTradeDTO() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        assertEquals(1, dto.getTradeId());
        assertEquals("2026-09-25T10:30:00", dto.getTradeTime());
        assertEquals(2, dto.getAccountId());
        assertEquals(3, dto.getInstrumentId());
        assertEquals("BUY", dto.getTradeType());
        assertEquals(BigDecimal.valueOf(100), dto.getQuantity());
        assertEquals(BigDecimal.valueOf(150), dto.getPrice());
        assertEquals("EXECUTED", dto.getStatus());
    }

    @Test
    @DisplayName("Should validate positive tradeId")
    void testValidateTradeId() {
        AccountTradeDTO dto = new AccountTradeDTO(-1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tradeId")));
    }

    @Test
    @DisplayName("Should validate non-blank tradeTime")
    void testValidateBlankTradeTime() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "   ", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tradeTime")));
    }

    @Test
    @DisplayName("Should validate positive accountId")
    void testValidateAccountId() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", -2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountId")));
    }

    @Test
    @DisplayName("Should validate positive instrumentId")
    void testValidateInstrumentId() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, -3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("instrumentId")));
    }

    @Test
    @DisplayName("Should validate non-blank tradeType")
    void testValidateBlankTradeType() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("tradeType")));
    }

    @Test
    @DisplayName("Should validate positive quantity")
    void testValidateQuantity() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(-100), BigDecimal.valueOf(150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
    }

    @Test
    @DisplayName("Should validate positive price")
    void testValidatePrice() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(-150), "EXECUTED");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    @DisplayName("Should validate non-blank status")
    void testValidateBlankStatus() {
        AccountTradeDTO dto = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "");
        
        Set<ConstraintViolation<AccountTradeDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        AccountTradeDTO dto1 = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        AccountTradeDTO dto2 = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "BUY", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        AccountTradeDTO dto3 = new AccountTradeDTO(1, "2026-09-25T10:30:00", 2, 3, "SELL", 
            BigDecimal.valueOf(100), BigDecimal.valueOf(150), "EXECUTED");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}
