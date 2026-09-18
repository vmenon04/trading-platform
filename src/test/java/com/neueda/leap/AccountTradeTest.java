package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTradeTest {

    private AccountTrade trade;
    private int accountId = 101;
    private int instrumentId = 1;

    @BeforeEach
    void setUp() {
        trade = new AccountTrade(1001, accountId, instrumentId, TradeType.BUY,
                100, 150.25, TradeStatus.ACCEPTED);
    }

    // BR-04: Client can submit order
    @Test
    void testTradeCanBeCreatedWithValidData() {
        assertNotNull(trade);
        assertEquals(1001, trade.getTradeId());
        assertEquals(accountId, trade.getAccountId());
        assertEquals(instrumentId, trade.getInstrumentId());
        assertEquals(100, trade.getQuantity());
        assertEquals(150.25, trade.getPrice());
    }

    // BR-05: Order validation before acceptance
    @Test
    void testTradeCannotHaveNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1002, accountId, instrumentId, TradeType.BUY,
                        -50, 150.25, TradeStatus.SUBMITTED));
    }

    @Test
    void testTradeCannotHaveNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1003, accountId, instrumentId, TradeType.BUY,
                        100, -150.25, TradeStatus.SUBMITTED));
    }

    @Test
    void testTradeCannotHaveZeroQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountTrade(1004, accountId, instrumentId, TradeType.BUY,
                        0, 150.25, TradeStatus.SUBMITTED));
    }

    @Test
    void testValidateTradeChecksAccountHasSufficientCash() {
        Account account = new Account(101, "Trading", 1);
        account.depositCash(5000.0);

        // Try to buy 100 shares at 150.25 = 15025.00 (exceeds 5000.00)
        boolean isValid = trade.validateTrade(account);
        assertFalse(isValid);
    }

    @Test
    void testValidateTradePassesWithSufficientCash() {
        Account account = new Account(101, "Trading", 1);
        account.depositCash(20000.0);

        boolean isValid = trade.validateTrade(account);
        assertTrue(isValid);
    }

    // BR-06: Order recorded before execution
    @Test
    void testTradeStatusStartsAsSubmitted() {
        AccountTrade newTrade = new AccountTrade(1005, accountId, instrumentId,
                TradeType.BUY, 50, 100.0,
                TradeStatus.SUBMITTED);
        assertEquals(TradeStatus.SUBMITTED, newTrade.getStatus());
    }

    @Test
    void testTradeRecordingIsImmutable() {
        Instant createdTime = Instant.now();
        AccountTrade recordedTrade = new AccountTrade(1006, accountId, instrumentId,
                TradeType.BUY, 100, 150.25,
                TradeStatus.ACCEPTED);
        recordedTrade.recordCreationTime(createdTime);

        assertEquals(createdTime, recordedTrade.getCreatedTime());
        // Attempt to change should fail or be ignored
        assertThrows(UnsupportedOperationException.class,
                () -> recordedTrade.recordCreationTime(Instant.now()));
    }

    // BR-07: Order status changes
    @Test
    void testTradeStatusProgression() {
        AccountTrade newTrade = new AccountTrade(1007, accountId, instrumentId,
                TradeType.BUY, 100, 150.25,
                TradeStatus.SUBMITTED);

        assertEquals(TradeStatus.SUBMITTED, newTrade.getStatus());
        newTrade.updateStatus(TradeStatus.ACCEPTED);
        assertEquals(TradeStatus.ACCEPTED, newTrade.getStatus());
        newTrade.updateStatus(TradeStatus.FILLED);
        assertEquals(TradeStatus.FILLED, newTrade.getStatus());
    }

    @Test
    void testTradeCanBeRejected() {
        AccountTrade newTrade = new AccountTrade(1008, accountId, instrumentId,
                TradeType.BUY, 100, 150.25,
                TradeStatus.SUBMITTED);
        newTrade.updateStatus(TradeStatus.REJECTED);
        assertEquals(TradeStatus.REJECTED, newTrade.getStatus());
    }

    // BR-08: Pricing based on market quote
    @Test
    void testTradePriceIsSetAtExecution() {
        MarketQuote quote = new MarketQuote(instrumentId, 155.50, 155.75);
        trade.setPriceFromQuote(quote);
        // Price should be execution price, not submitted price
        assertEquals(155.75, trade.getExecutionPrice()); // bid or ask based on trade type
    }

    // BR-15: Full lifecycle reconstruction
    @Test
    void testTradeAuditTrailContainsAllsteps() {
        AccountTrade auditedTrade = new AccountTrade(1009, accountId, instrumentId,
                TradeType.BUY, 100, 150.25,
                TradeStatus.SUBMITTED);

        auditedTrade.recordCreationTime(Instant.now());
        auditedTrade.updateStatus(TradeStatus.ACCEPTED);
        auditedTrade.updateStatus(TradeStatus.FILLED);

        AuditTrail trail = auditedTrade.getAuditTrail();
        assertNotNull(trail);
        assertEquals(4, trail.getEvents().size()); // Created, Submitted, Accepted, Filled
    }
}