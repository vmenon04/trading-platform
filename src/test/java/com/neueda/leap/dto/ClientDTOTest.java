package com.neueda.leap.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.neueda.leap.enums.AccountType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClientDTO Tests")
class ClientDTOTest {
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
    @DisplayName("Should create ClientDTO with valid data")
    void testValidClientDTO() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "John", "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        assertEquals(1, dto.getClientId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals(LocalDate.of(1990, 1, 15), dto.getBirthDate());
        assertEquals(1, dto.getClientAccounts().size());
    }

    @Test
    @DisplayName("Should validate positive clientId")
    void testValidateClientId() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(-1, "John", "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("clientId")));
    }

    @Test
    @DisplayName("Should validate non-blank firstName")
    void testValidateBlankFirstName() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "   ", "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    @DisplayName("Should validate null firstName")
    void testValidateNullFirstName() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, null, "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    @DisplayName("Should validate non-blank lastName")
    void testValidateBlankLastName() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "John", "", LocalDate.of(1990, 1, 15), accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
    }

    @Test
    @DisplayName("Should validate birthDate is in the past")
    void testValidateFutureBirthDate() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "John", "Doe", LocalDate.now().plusDays(1), accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("birthDate")));
    }

    @Test
    @DisplayName("Should validate null birthDate")
    void testValidateNullBirthDate() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "John", "Doe", null, accounts);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("birthDate")));
    }

    @Test
    @DisplayName("Should validate null clientAccounts")
    void testValidateNullClientAccounts() {
        ClientDTO dto = new ClientDTO(1, "John", "Doe", LocalDate.of(1990, 1, 15), null);
        
        Set<ConstraintViolation<ClientDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("clientAccounts")));
    }

    @Test
    @DisplayName("Should support record equality")
    void testRecordEquality() {
        List<AccountDTO> accounts1 = List.of(new AccountDTO(1, AccountType.SAVINGS));
        List<AccountDTO> accounts2 = List.of(new AccountDTO(1, AccountType.SAVINGS));
        LocalDate birthDate = LocalDate.of(1990, 1, 15);
        
        ClientDTO dto1 = new ClientDTO(1, "John", "Doe", birthDate, accounts1);
        ClientDTO dto2 = new ClientDTO(1, "John", "Doe", birthDate, accounts2);
        ClientDTO dto3 = new ClientDTO(1, "Jane", "Doe", birthDate, accounts1);
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
    }

    @Test
    @DisplayName("Should support record immutability")
    void testRecordImmutability() {
        List<AccountDTO> accounts = List.of(new AccountDTO(1, AccountType.SAVINGS));
        ClientDTO dto = new ClientDTO(1, "John", "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        assertEquals("John", dto.getFirstName());
        // Records are immutable, no setters exist
    }

    @Test
    @DisplayName("Should support multiple account types")
    void testMultipleAccountTypes() {
        List<AccountDTO> accounts = List.of(
            new AccountDTO(1, AccountType.SAVINGS),
            new AccountDTO(2, AccountType.BROKERAGE)
        );
        ClientDTO dto = new ClientDTO(1, "John", "Doe", LocalDate.of(1990, 1, 15), accounts);
        
        assertEquals(2, dto.getClientAccounts().size());
        assertEquals(AccountType.SAVINGS, dto.getClientAccounts().get(0).getAccountType());
        assertEquals(AccountType.BROKERAGE, dto.getClientAccounts().get(1).getAccountType());
    }
}

