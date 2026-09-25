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
class OrderRequestDTOTest {
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
    void testValidOrderRequestDTO() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 2, "BUY", new BigDecimal("150.50"));
        
        assertEquals(1, dto.getAccountId());
        assertEquals(2, dto.getInstrumentId());
        assertEquals("BUY", dto.getSide());
        assertEquals(new BigDecimal("150.50"), dto.getQuantity());
    }

    @Test
    void testValidatePositiveAccountId() {
        OrderRequestDTO dto = new OrderRequestDTO(
            -1, 2, "BUY", new BigDecimal("150.50"));
        Set<ConstraintViolation<OrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveInstrumentId() {
        OrderRequestDTO dto = new OrderRequestDTO(
            1, 0, "BUY", new BigDecimal("150.50"));
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
        
        assertEquals("BUY", buyOrder.getSide());
        assertEquals("SELL", sellOrder.getSide());
        assertNotEquals(buyOrder, sellOrder);
    }
}

