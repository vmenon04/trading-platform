package com.neueda.leap.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void noArgsConstructorInitializesDefaults() {
        Account account = new Account();

        assertAll(
                () -> assertNull(account.getAccountId()),
                () -> assertEquals(BigDecimal.ZERO, account.getBalance()),
                () -> assertNotNull(account.getHoldings()),
                () -> assertTrue(account.getHoldings().isEmpty())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValues() {
        Account account = new Account(42L);

        assertAll(
                () -> assertNull(account.getAccountId()),
                () -> assertEquals(BigDecimal.ZERO, account.getBalance()),
                () -> assertNotNull(account.getHoldings()),
                () -> assertTrue(account.getHoldings().isEmpty())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        Account account = new Account();
        HashMap<Instrument, AccountHolding> holdings = new HashMap<>();

        account.setAccountId(10L);
        account.setBalance(BigDecimal.valueOf(99.5));
        account.setHoldings(holdings);

        assertAll(
                () -> assertEquals(10L, account.getAccountId()),
                () -> assertEquals(BigDecimal.valueOf(99.5), account.getBalance()),
                () -> assertSame(holdings, account.getHoldings())
        );
    }
}