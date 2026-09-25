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

@DisplayName("InstrumentDTO Tests")
class InstrumentDTOTest {
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
    void testValidInstrumentDTO() {
        InstrumentDTO dto = new InstrumentDTO(1, "Apple", "AAPL");
        assertEquals(1, dto.getInstrumentId());
        assertEquals("Apple", dto.getName());
    }

    @Test
    void testValidateInstrumentId() {
        InstrumentDTO dto = new InstrumentDTO(-1, "Apple", "AAPL");
        Set<ConstraintViolation<InstrumentDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateBlankName() {
        InstrumentDTO dto = new InstrumentDTO(1, "   ", "AAPL");
        Set<ConstraintViolation<InstrumentDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testValidateBlankTicker() {
        InstrumentDTO dto = new InstrumentDTO(1, "Apple", "");
        Set<ConstraintViolation<InstrumentDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testRecordEquality() {
        InstrumentDTO dto1 = new InstrumentDTO(1, "Apple", "AAPL");
        InstrumentDTO dto2 = new InstrumentDTO(1, "Apple", "AAPL");
        InstrumentDTO dto3 = new InstrumentDTO(2, "Microsoft", "MSFT");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }
}

