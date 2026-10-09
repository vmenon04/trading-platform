package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.neueda.leap.enums.TradeSide;
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
class TradeRequestDTOTest {

    private Validator validator;

    private TradeRequestDTO dto;

    private UUID external_account_id;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()) {
            validator = factory.getValidator();
        }
        external_account_id = UUID.randomUUID();
        dto = new TradeRequestDTO(external_account_id, 2L, TradeSide.BUY, new BigDecimal("150.50"));
    }

    @Test
    void testValidOrderRequestDTO() {
        
        assertEquals(external_account_id, dto.accountId());
        assertEquals(2L, dto.instrumentId());
        assertEquals(TradeSide.BUY, dto.side());
        assertEquals(new BigDecimal("150.50"), dto.quantity());
    }

    @Test
    void testValidatePositiveInstrumentId() {
        TradeRequestDTO invalidInstrumentDto = new TradeRequestDTO(
            external_account_id, invalidInstrumentId(), TradeSide.BUY, new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(invalidInstrumentDto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNullSide() {
        TradeRequestDTO dto = new TradeRequestDTO(
            external_account_id, 2L, null, new BigDecimal("150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, new BigDecimal("-150.50"));
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonNullQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, null);
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateZeroQuantity() {
        TradeRequestDTO dto = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, BigDecimal.ZERO);
        Set<ConstraintViolation<TradeRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecordEquality() {
        TradeRequestDTO dto1 = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, new BigDecimal("150.50"));
        TradeRequestDTO dto2 = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, new BigDecimal("150.50"));
        TradeRequestDTO dto3 = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.SELL, new BigDecimal("150.50"));
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    void testSupportsBuyAndSellSides() {
        TradeRequestDTO buyOrder = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.BUY, new BigDecimal("100"));
        TradeRequestDTO sellOrder = new TradeRequestDTO(
            external_account_id, 2L, TradeSide.SELL, new BigDecimal("100"));
        
        assertEquals(TradeSide.BUY, buyOrder.side());
        assertEquals(TradeSide.SELL, sellOrder.side());
        assertNotEquals(buyOrder, sellOrder);
    }

    private String invalidAccountId() {
        return "invalid-uuid";
    }

    private Long invalidInstrumentId() {
        return Long.parseLong("-1");
    }
}

