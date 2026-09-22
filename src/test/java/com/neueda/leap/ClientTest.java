package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    private Client client;

    @BeforeEach
    void setUp() {
        // change this test to use first name and last name separately
        client = new Client(1, "John Doe", LocalDate.of(1990, 5, 15));
    }

    // BR-01: Client Registration
    @Test
    void testClientCreationWithValidData() {
        assertNotNull(client);
        assertEquals(1, client.getClientId());
        assertEquals("John Doe", client.getName()); //getName() should be split for first/last name
        assertEquals(LocalDate.of(1990, 5, 15), client.getBirthDate());
    }

    @Test
    void testClientRegistrationRequiresName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Client(1, null, LocalDate.of(1990, 5, 15)));
    }

    @Test
    void testClientRegistrationRequiresBirthDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Client(1, "John Doe", null));
    }

    @Test
    void testClientCannotHaveFutureAsBirthDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new Client(1, "John Doe", LocalDate.now().plusYears(1)));
    }

    // BR-02: Clients can only access their own data
    @Test
    void testClientIdMustBeUnique() {
        Client client2 = new Client(2, "Jane Smith", LocalDate.of(1988, 3, 20));
        assertNotEquals(client.getClientId(), client2.getClientId());
    }

    @Test
    void testClientCanViewOwnAccounts() {
        Account account1 = new Account(101, "Trading", client.getClientId());
        Account account2 = new Account(102, "Cash", client.getClientId());

        client.addAccount(account1);
        client.addAccount(account2);

        assertEquals(2, client.getAccounts().size());
        assertTrue(client.getAccounts().stream()
                .allMatch(a -> a.getClientId() == client.getClientId()));
    }

}