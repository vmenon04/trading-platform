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

@DisplayName("ErrorResponseDTO Tests")
class ErrorResponseDTOTest {
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
    @DisplayName("Should create ErrorResponseDTO with valid data")
    void testValidErrorResponseDTO() {
        ErrorResponseDTO dto = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Account does not have sufficient balance");
        
        assertEquals("INSUFFICIENT_FUNDS", dto.error());
        assertEquals("Account does not have sufficient balance", dto.message());
    }

    @Test
    @DisplayName("Should validate non-blank error field")
    void testValidateBlankError() {
        ErrorResponseDTO dto = new ErrorResponseDTO("   ", "Account does not have sufficient balance");
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("error")));
    }

    @Test
    @DisplayName("Should validate null error field")
    void testValidateNullError() {
        ErrorResponseDTO dto = new ErrorResponseDTO(null, "Account does not have sufficient balance");
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("error")));
    }

    @Test
    @DisplayName("Should validate non-blank message field")
    void testValidateBlankMessage() {
        ErrorResponseDTO dto = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "");
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("message")));
    }

    @Test
    @DisplayName("Should validate null message field")
    void testValidateNullMessage() {
        ErrorResponseDTO dto = new ErrorResponseDTO("INSUFFICIENT_FUNDS", null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("message")));
    }

    @Test
    @DisplayName("Should validate both fields when both are blank")
    void testValidateBothBlank() {
        ErrorResponseDTO dto = new ErrorResponseDTO("  ", "   ");
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        ErrorResponseDTO dto1 = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Low balance");
        ErrorResponseDTO dto2 = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Low balance");
        ErrorResponseDTO dto3 = new ErrorResponseDTO("INVALID_ACCOUNT", "Account not found");
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    @DisplayName("Should support record hashCode")
    void testRecordHashCode() {
        ErrorResponseDTO dto1 = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Low balance");
        ErrorResponseDTO dto2 = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Low balance");
        
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("Should support different error codes")
    void testDifferentErrorCodes() {
        ErrorResponseDTO invalidAccount = new ErrorResponseDTO("INVALID_ACCOUNT", "Account not found");
        ErrorResponseDTO insufficientFunds = new ErrorResponseDTO("INSUFFICIENT_FUNDS", "Low balance");
        ErrorResponseDTO invalidInstrument = new ErrorResponseDTO("INVALID_INSTRUMENT", "Instrument not available");
        
        assertNotEquals(invalidAccount, insufficientFunds);
        assertNotEquals(insufficientFunds, invalidInstrument);
        assertNotEquals(invalidAccount, invalidInstrument);
    }

    @Test
    @DisplayName("Should preserve immutability of record")
    void testRecordImmutability() {
        ErrorResponseDTO dto = new ErrorResponseDTO("ERROR_CODE", "Error message");
        
        assertEquals("ERROR_CODE", dto.error());
        assertEquals("Error message", dto.message());
        // Records are immutable, accessor methods return the record components
    }
}

