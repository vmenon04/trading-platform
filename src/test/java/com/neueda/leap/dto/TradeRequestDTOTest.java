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

@DisplayName("OrderRequestDTO Tests")
class TradeRequestDTOTest {
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
    void testValidOrderRequestDTO() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, "BUY", new BigDecimal("150.50"));
        
        assertEquals(1L, dto.accountId());
        assertEquals(2L, dto.instrumentId());
        assertEquals("BUY", dto.side());
        assertEquals(new BigDecimal("150.50"), dto.quantity());
    }

    @Test
    void testValidatePositiveAccountId() {
        TradeRequestDTO dto = new TradeRequestDTO(
            invalidAccountId(), 2L, "BUY", new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveInstrumentId() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, invalidInstrumentId(), "BUY", new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonBlankSide() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, "   ", new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNullSide() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, null, new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, "BUY", new BigDecimal("-150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonNullQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, "BUY", null);
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateZeroQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            1L, 2L, "BUY", BigDecimal.ZERO);
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecordEquality() {
        TradeRequestDTO dto1 = new TradeRequestDTO(
            1L, 2L, "BUY", new BigDecimal("150.50"));
        TradeRequestDTO dto2 = new TradeRequestDTO(
            1L, 2L, "BUY", new BigDecimal("150.50"));
        TradeRequestDTO dto3 = new TradeRequestDTO(
            1L, 2L, "SELL", new BigDecimal("150.50"));
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    void testSupportsBuyAndSellSides() {
        TradeRequestDTO buyOrder = new TradeRequestDTO(
            1L, 2L, "BUY", new BigDecimal("100"));
        TradeRequestDTO sellOrder = new TradeRequestDTO(
            1L, 2L, "SELL", new BigDecimal("100"));
        
        assertEquals("BUY", buyOrder.side());
        assertEquals("SELL", sellOrder.side());
        assertNotEquals(buyOrder, sellOrder);
    }

    private Long invalidAccountId() {
        return Long.parseLong("-1");
    }

    private Long invalidInstrumentId() {
        return Long.parseLong("-1");
    }
}

