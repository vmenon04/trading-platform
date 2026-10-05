package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrderRequestDTO Tests")
class OrderRequestDTOTest {
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
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("150.50"));
        
        assertEquals(1, dto.accountId());
        assertEquals(2, dto.instrumentId());
        assertEquals("BUY", dto.side());
        assertEquals(new BigDecimal("150.50"), dto.quantity());
    }

    @Test
    void testValidatePositiveAccountId() {
        OrderRequestDTO dto = new OrderRequestDTO(
            invalidAccountId(), 2, "BUY", new BigDecimal("150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveInstrumentId() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, invalidInstrumentId(), "BUY", new BigDecimal("150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonBlankSide() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "   ", new BigDecimal("150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNullSide() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, null, new BigDecimal("150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveQuantity() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("-150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonNullQuantity() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "BUY", null);
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateZeroQuantity() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "BUY", BigDecimal.ZERO);
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecordEquality() {
        OrderRequestDTO dto1 = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("150.50"));
        OrderRequestDTO dto2 = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("150.50"));
        OrderRequestDTO dto3 = new OrderRequestDTO(
            1, 2, "SELL", new BigDecimal("150.50"));
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    void testSupportsBuyAndSellSides() {
        OrderRequestDTO buyOrder = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("100"));
        OrderRequestDTO sellOrder = new OrderRequestDTO(
            1, 2, "SELL", new BigDecimal("100"));
        
        assertEquals("BUY", buyOrder.side());
        assertEquals("SELL", sellOrder.side());
        assertNotEquals(buyOrder, sellOrder);
    }

    private int invalidAccountId() {
        return Integer.parseInt("-1");
    }

    private int invalidInstrumentId() {
        return Integer.parseInt("-1");
    }
}

