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
import org.springframework.http.HttpStatus;

@DisplayName("ErrorResponseDTO Tests")
class ErrorResponseDTOTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try( ValidatorFactory factory = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()) {
            validator = factory.getValidator();
        } catch (Exception e) {
            fail("Failed to set up validator: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should create ErrorResponseDTO with valid data")
    void testValidErrorResponseDTO() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Account does not have sufficient balance", null);
        
        assertEquals("INSUFFICIENT_FUNDS", dto.error());
        assertEquals("Account does not have sufficient balance", dto.message());
    }

    @Test
    @DisplayName("Should validate non-blank error field")
    void testValidateBlankError() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "   ", "Account does not have sufficient balance", null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("error")));
    }

    @Test
    @DisplayName("Should validate null error field")
    void testValidateNullError() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, null, "Account does not have sufficient balance", null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("error")));
    }

    @Test
    @DisplayName("Should validate non-blank message field")
    void testValidateBlankMessage() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "", null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("message")));
    }

    @Test
    @DisplayName("Should validate null message field")
    void testValidateNullMessage() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", null, null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("message")));
    }

    @Test
    @DisplayName("Should validate both fields when both are blank")
    void testValidateBothBlank() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "  ", "   ", null);
        
        Set<ConstraintViolation<ErrorResponseDTO>> violations = validator.validate(dto);
        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        ErrorResponseDTO dto1 = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Low balance", null);
        ErrorResponseDTO dto2 = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Low balance", null);
        ErrorResponseDTO dto3 = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT", "Account not found", null);
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    @DisplayName("Should support record hashCode")
    void testRecordHashCode() {
        ErrorResponseDTO dto1 = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Low balance", null);
        ErrorResponseDTO dto2 = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Low balance", null);
        
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("Should support different error codes")
    void testDifferentErrorCodes() {
        ErrorResponseDTO invalidAccount = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT", "Account not found", null);
        ErrorResponseDTO insufficientFunds = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", "Low balance", null);
        ErrorResponseDTO invalidInstrument = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "INVALID_INSTRUMENT", "Instrument not available", null);
        
        assertNotEquals(invalidAccount, insufficientFunds);
        assertNotEquals(insufficientFunds, invalidInstrument);
        assertNotEquals(invalidAccount, invalidInstrument);
    }

    @Test
    @DisplayName("Should preserve immutability of record")
    void testRecordImmutability() {
        ErrorResponseDTO dto = new ErrorResponseDTO(HttpStatus.BAD_REQUEST, "ERROR_CODE", "Error message", null);
        
        assertEquals("ERROR_CODE", dto.error());
        assertEquals("Error message", dto.message());
        // Records are immutable, accessor methods return the record components
    }
}

