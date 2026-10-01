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

@DisplayName("ModelPortfolioHoldingDTO Tests")
class ModelPortfolioHoldingDTOTest {
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
    void testValidModelPortfolioHoldingDTO() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 25.5, "ACTIVE");
        
        assertEquals(1, dto.getModelPortfolioId());
        assertEquals(2, dto.getInstrumentId());
        assertEquals("2026-09-25", dto.getEffectiveDate());
        assertEquals(25.5, dto.getTargetWeightPct(), 0.01);
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    void testValidatePositiveModelPortfolioId() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            -1, 2, "2026-09-25", 25.5, "ACTIVE");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveInstrumentId() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, -2, "2026-09-25", 25.5, "ACTIVE");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateNonBlankEffectiveDate() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, 2, "", 25.5, "ACTIVE");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidatePositiveOrZeroWeightPct() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", -5.0, "ACTIVE");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateZeroWeightPctAllowed() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 0.0, "ACTIVE");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidateNonBlankStatus() {
        ModelPortfolioHoldingDTO dto = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 25.5, "   ");
        Set<ConstraintViolation<ModelPortfolioHoldingDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecordEquality() {
        ModelPortfolioHoldingDTO dto1 = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 25.5, "ACTIVE");
        ModelPortfolioHoldingDTO dto2 = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 25.5, "ACTIVE");
        ModelPortfolioHoldingDTO dto3 = new ModelPortfolioHoldingDTO(
            1, 2, "2026-09-25", 30.0, "ACTIVE");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}

