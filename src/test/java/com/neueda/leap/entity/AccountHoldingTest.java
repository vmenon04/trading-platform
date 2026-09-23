package com.neueda.leap.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AccountHoldingTest {

    private AccountHolding holding;
    private final int ACCOUNT_ID = 101;
    private final int INSTRUMENT_ID = 1;
    private final double  QUANTITY = 100;

    @BeforeEach
    void setUp() {
        //Account_Holding(accountID, instrumentID, quantity, status)
        holding = new AccountHolding(ACCOUNT_ID, INSTRUMENT_ID, QUANTITY, HoldingStatus.ACTIVE);
    }

    // BR-10: Client can see current holdings
    @Test
    void testHoldingCanBeCreated() {
        assertNotNull(holding);
        assertEquals(ACCOUNT_ID, holding.getAccountId());
        assertEquals(INSTRUMENT_ID, holding.getInstrumentId());
        assertEquals(QUANTITY, holding.getQuantity());
        assertEquals(HoldingStatus.ACTIVE, holding.getStatus());
    }

    // BR-09: Atomic updates
    @Test
    void testHoldingQuantityUpdateOnBuy() {
        holding.updateQuantity(50, "BUY");
        assertEquals(150, holding.getQuantity());
    }

    @Test
    void testHoldingQuantityUpdateOnSell() {
        holding.updateQuantity(50, "SELL");
        assertEquals(50, holding.getQuantity());
    }

    @Test
    void testHoldingCannotGoNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> holding.updateQuantity(150, "SELL"));
    }

    @Test
    void testHoldingTransitionsOnFullSell() {
        assertEquals(HoldingStatus.ACTIVE, holding.getStatus());
        holding.updateQuantity(100, "SELL");
        assertEquals(HoldingStatus.INACTIVE, holding.getStatus());
    }

    @Test
    void testHoldingHistory() {
        String outputString = holding.toString();
        assertEquals(outputString, holding.getHistory());
        holding.updateQuantity(50, "SELL");
        outputString += "\n" + holding.toString();
        assertEquals(outputString, holding.getHistory());
    }
}