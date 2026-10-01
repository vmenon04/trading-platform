package com.neueda.leap.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void noArgsConstructorInitializesClientAccounts() {
        Client client = new Client();

        assertAll(
                () -> assertNull(client.getClientId()),
                () -> assertNull(client.getFirstName()),
                () -> assertNull(client.getLastName()),
                () -> assertNull(client.getBirthDate()),
                () -> assertNotNull(client.getClientAccounts()),
                () -> assertTrue(client.getClientAccounts().isEmpty())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        Client client = new Client("John", "Doe", birthDate);

        assertAll(
                () -> assertNull(client.getClientId()),
                () -> assertEquals("John", client.getFirstName()),
                () -> assertEquals("Doe", client.getLastName()),
                () -> assertEquals(birthDate, client.getBirthDate()),
                () -> assertNotNull(client.getClientAccounts()),
                () -> assertTrue(client.getClientAccounts().isEmpty())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        Client client = new Client();
        LocalDate birthDate = LocalDate.of(1988, 3, 20);
        ArrayList<Account> accounts = new ArrayList<>();

        client.setClientId(5L);
        client.setFirstName("Jane");
        client.setLastName("Smith");
        client.setBirthDate(birthDate);
        client.setClientAccounts(accounts);

        assertAll(
                () -> assertEquals(5L, client.getClientId()),
                () -> assertEquals("Jane", client.getFirstName()),
                () -> assertEquals("Smith", client.getLastName()),
                () -> assertEquals(birthDate, client.getBirthDate()),
                () -> assertSame(accounts, client.getClientAccounts())
        );
    }

}