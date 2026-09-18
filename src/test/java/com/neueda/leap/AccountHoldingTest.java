package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class AccountHoldingTest {

    private AccountHolding holding;
    private int accountId = 101;
    private int instrumentId = 1;

    @BeforeEach
    void setUp() {
        //Account_Holding(accountID, instrumentID, asOfDate, quantity, status)
        holding = new AccountHolding(accountId, instrumentId, LocalDate.now(),
                100, HoldingStatus.ACTIVE);
    }

    // BR-10: Client can see current holdings
    @Test
    void testHoldingCanBeCreated() {
        assertNotNull(holding);
        assertEquals(accountId, holding.getAccountId());
        assertEquals(instrumentId, holding.getInstrumentId());
        assertEquals(100, holding.getQuantity());
        assertEquals(HoldingStatus.ACTIVE, holding.getStatus());
    }

    @Test
    void testHoldingQuantityCanBeQueried() {
        assertEquals(100, holding.getQuantity());
    }

    @Test
    void testHoldingAssociatedWithCorrectAsOfDate() {
        LocalDate asOfDate = LocalDate.now();
        AccountHolding timedHolding = new AccountHolding(accountId, instrumentId,
                asOfDate, 50, HoldingStatus.ACTIVE);
        assertEquals(asOfDate, timedHolding.getAsOfDate());
    }

    // BR-09: Atomic updates
    @Test
    void testHoldingQuantityCanBeUpdatedAtomically() {
        holding.updateQuantity(50, "BUY");
        assertEquals(150, holding.getQuantity());
    }

    @Test
    void testHoldingCannotGoNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> holding.updateQuantity(-50, "BUY"));
    }

    @Test
    void testHoldingStatusCanTransition() {
        assertEquals(HoldingStatus.ACTIVE, holding.getStatus());
        holding.updateQuantity(100, "SELL");
        assertEquals(HoldingStatus.INACTIVE, holding.getStatus());
    }
}