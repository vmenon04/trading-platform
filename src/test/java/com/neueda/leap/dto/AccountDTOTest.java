package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.neueda.leap.enums.AccountType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountDTO Tests")
class AccountDTOTest {
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
    @DisplayName("Should create AccountDTO with valid data")
    void testValidAccountDTO() {
        AccountDTO dto = new AccountDTO(1, AccountType.SAVINGS);
        
        assertEquals(1, dto.getAccountId());
        assertEquals(AccountType.SAVINGS, dto.getAccountType());
    }

    @Test
    @DisplayName("Should validate positive accountId")
    void testValidateAccountId() {
        AccountDTO dto = new AccountDTO(-1, AccountType.SAVINGS);
        
        Set<ConstraintViolation<AccountDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountId")));
    }

    @Test
    @DisplayName("Should validate null accountId")
    void testValidateNullAccountId() {
        AccountDTO dto = new AccountDTO(null, AccountType.SAVINGS);
        
        Set<ConstraintViolation<AccountDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountId")));
    }

    @Test
    @DisplayName("Should validate non-null accountType")
    void testValidateNullAccountType() {
        AccountDTO dto = new AccountDTO(1, null);
        
        Set<ConstraintViolation<AccountDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("accountType")));
    }

    @Test
    @DisplayName("Should support both SAVINGS and BROKERAGE account types")
    void testValidAccountTypes() {
        AccountDTO savingsDto = new AccountDTO(1, AccountType.SAVINGS);
        AccountDTO brokerageDto = new AccountDTO(2, AccountType.BROKERAGE);
        
        assertEquals(AccountType.SAVINGS, savingsDto.getAccountType());
        assertEquals(AccountType.BROKERAGE, brokerageDto.getAccountType());
        assertNotEquals(savingsDto.getAccountType(), brokerageDto.getAccountType());
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        AccountDTO dto1 = new AccountDTO(1, AccountType.SAVINGS);
        AccountDTO dto2 = new AccountDTO(1, AccountType.SAVINGS);
        AccountDTO dto3 = new AccountDTO(2, AccountType.BROKERAGE);
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    @DisplayName("Should support record hashCode")
    void testRecordHashCode() {
        AccountDTO dto1 = new AccountDTO(1, AccountType.SAVINGS);
        AccountDTO dto2 = new AccountDTO(1, AccountType.SAVINGS);
        
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}

