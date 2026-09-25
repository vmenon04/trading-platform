package com.neueda.leap.entity;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void noArgsConstructorInitializesDefaults() {
        Account account = new Account();

        assertAll(
                () -> assertNull(account.getAccountId()),
                () -> assertNull(account.getAccountType()),
                () -> assertEquals(0, account.getClientId()),
                () -> assertEquals(0.0, account.getBalance()),
                () -> assertNotNull(account.getHoldings()),
                () -> assertTrue(account.getHoldings().isEmpty())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        Account account = new Account("Trading", 42);

        assertAll(
                () -> assertNull(account.getAccountId()),
                () -> assertEquals("Trading", account.getAccountType()),
                () -> assertEquals(42, account.getClientId()),
                () -> assertEquals(0.0, account.getBalance()),
                () -> assertNotNull(account.getHoldings()),
                () -> assertTrue(account.getHoldings().isEmpty())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        Account account = new Account();
        HashMap<Instrument, AccountHolding> holdings = new HashMap<>();

        account.setAccountId(10L);
        account.setAccountType("Cash");
        account.setClientId(7);
        account.setBalance(99.5);
        account.setHoldings(holdings);

        assertAll(
                () -> assertEquals(10L, account.getAccountId()),
                () -> assertEquals("Cash", account.getAccountType()),
                () -> assertEquals(7, account.getClientId()),
                () -> assertEquals(99.5, account.getBalance()),
                () -> assertSame(holdings, account.getHoldings())
        );
    }
}