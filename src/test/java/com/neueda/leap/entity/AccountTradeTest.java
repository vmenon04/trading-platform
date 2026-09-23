package com.neueda.leap.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTradeTest {

    private AccountTrade trade;
    private final int ACCOUNT_ID = 101;
    private final int INSTRUMENT_ID = 1;

    private final int TRADE_ID = 1001;
    private final double QUANTITY = 100;
    private final double PRICE = 150.25;

    @BeforeEach
    void setUp() {
        trade = new AccountTrade(TRADE_ID, ACCOUNT_ID, INSTRUMENT_ID, TradeType.BUY,
                QUANTITY, PRICE);
    }

    // BR-04: Client can submit order
    @Test
    void testTradeCanBeCreatedWithValidData() {
        assertNotNull(trade);
        assertEquals(TRADE_ID, trade.getTradeId());
        assertEquals(ACCOUNT_ID, trade.getAccountId());
        assertEquals(INSTRUMENT_ID, trade.getInstrumentId());
        assertEquals(TradeType.BUY, trade.getTradeType());
        assertEquals(QUANTITY, trade.getQuantity());
        assertEquals(PRICE, trade.getPrice());
        assertEquals(TradeStatus.PENDING, trade.getStatus());
    }

    // BR-05: Order validation before acceptance
    @Test
    void testTradeCannotHaveNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1002, ACCOUNT_ID, INSTRUMENT_ID, TradeType.BUY,
                        -QUANTITY, PRICE));
    }

    @Test
    void testTradeCannotHaveNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1002, ACCOUNT_ID, INSTRUMENT_ID, TradeType.BUY,
                        QUANTITY, -PRICE));
    }

    @Test
    void testTradeCannotHaveZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1002, ACCOUNT_ID, INSTRUMENT_ID, TradeType.BUY,
                        0, PRICE));
    }

    @Test
    void testTradeCannotHaveZeroPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1002, ACCOUNT_ID, INSTRUMENT_ID, TradeType.BUY,
                        QUANTITY, 0.0));
    }

    @Test
    void testValidateTradeChecksAccountHasSufficientCash() {
        Account account = new Account(101, "Trading", 1);
        account.deposit(5000.0);

        // Try to buy 100 shares at 150.25 = 15025.00 (exceeds 5000.00)
        boolean isValid = trade.validateTrade(account);
        assertFalse(isValid);
    }

    @Test
    void testValidateTradePassesWithSufficientCash() {
        Account account = new Account(101, "Trading", 1);
        account.deposit(20000.0);

        boolean isValid = trade.validateTrade(account);
        assertTrue(isValid);
    }

    @Test
    void testValidateTradePassesWithExactCash() {
        Account account = new Account(101, "Trading", 1);
        account.deposit(15025.0);

        boolean isValid = trade.validateTrade(account);
        assertTrue(isValid);
    }

    // BR-06: Order recorded before execution
    @Test
    void testTradeStatusStartsAsSubmitted() {
        AccountTrade newTrade = new AccountTrade(1005, ACCOUNT_ID, INSTRUMENT_ID,
                TradeType.BUY, QUANTITY, PRICE);
        assertEquals(TradeStatus.PENDING, newTrade.getStatus());
    }

    // BR-07: Order status changes
    @Test
    void testTradeStatusProgression() {
        assertEquals(TradeStatus.PENDING, trade.getStatus());
        newTrade.updateStatus(TradeStatus.ACCEPTED);
        assertEquals(TradeStatus.ACCEPTED, trade.getStatus());
        newTrade.updateStatus(TradeStatus.FULFILLED);
        assertEquals(TradeStatus.FULFILLED, trade.getStatus());
    }

    @Test
    void testTradeCanBeRejected() {
        assertEquals(TradeStatus.PENDING, trade.getStatus());
        newTrade.updateStatus(TradeStatus.REJECTED);
        assertEquals(TradeStatus.REJECTED, trade.getStatus());
    }

    @Test
    void testTradeCreationTime() {
        LocalDate start = LocalDate.now();
        AccountTrade auditedTrade = new AccountTrade(1009, ACCOUNT_ID, INSTRUMENT_ID,
                TradeType.BUY, QUANTITY, PRICE);
        LocalDate end = LocalDate.now();

        assertAll(
            () -> assertTrue(auditedTrade.getCreatedTime().after(start) || auditedTrade.getCreatedTime().equals(start)),
            () -> assertTrue(auditedTrade.getCreatedTime().before(end) || auditedTrade.getCreatedTime().equals(end))
        );
    }

    // BR-15: Full lifecycle reconstruction
    @Test
    void testFullValidTradeLifeCycle() {
        AccountTrade auditedTrade = new AccountTrade(1009, ACCOUNT_ID, INSTRUMENT_ID,
                TradeType.BUY, QUANTITY, PRICE);

        Date creationTime = auditedTrade.getCreationTime();

        assertNull(auditedTrade.getProcessTime());
        auditedTrade.updateStatus(TradeStatus.ACCEPTED);
        Date acceptedTime = auditedTrade.getProcessTime();

        assertTrue(creationTime.before(acceptedTime));

        assertNull(auditedTrade.getFulfilledTime());
        auditedTrade.updateStatus(TradeStatus.FILLED);
        Date fulfilledTime = auditedTrade.getFulfilledTime();

        assertTrue(acceptedTime.before(fulfilledTime));
    }

    @Test
    void testTradeHistory() {
        AccountTrade auditedTrade = new AccountTrade(1009, ACCOUNT_ID, INSTRUMENT_ID,
                TradeType.BUY, QUANTITY, PRICE);

        String outputString = auditedTrade.toString();
        assertEquals(outputString, auditedTrade.getHistory());

        auditedTrade.updateStatus(TradeStatus.ACCEPTED);
        outputString = outputString + ("\n" + auditedTrade.toString());
        assertEquals(outputString, auditedTrade.getHistory());

        auditedTrade.updateStatus(TradeStatus.FILLED);
        outputString = outputString + ("\n" + auditedTrade.toString());
        assertEquals(outputString, auditedTrade.getHistory());
    }
}