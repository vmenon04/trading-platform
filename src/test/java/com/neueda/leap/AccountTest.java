package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    private Account account;
    private int clientId = 1;

    @BeforeEach
    void setUp() {
        //Account(accountID, accountType, clientID)
        account = new Account(101, "Trading", clientId);
    }

    // BR-10: Client must be able to see current holdings and cash balance
    @Test
    void testAccountInitializationWithZeroCash() {
        assertEquals(0.0, account.getCashBalance());
    }

    @Test
    void testAccountCanDepositCash() {
        account.depositCash(10000.0);
        assertEquals(10000.0, account.getCashBalance());
    }

    @Test
    void testAccountCanWithdrawCash() {
        account.depositCash(10000.0);
        account.withdrawCash(5000.0);
        assertEquals(5000.0, account.getCashBalance());
    }

    @Test
    void testAccountCannotWithdrawMoreThanBalance() {
        account.depositCash(1000.0);
        assertThrows(IllegalArgumentException.class,
                () -> account.withdrawCash(2000.0));
    }

    @Test
    void testAccountStoresHoldings() {
        Instrument apple = new Instrument(1, "Apple", "AAPL");
        account.addHolding(apple, 100, 150.50);

        assertEquals(1, account.getHoldings().size());
        assertEquals(100, account.getHoldingQuantity(apple.getInstrumentId()));
    }

    // BR-02: Client isolation - accounts linked to specific client
    @Test
    void testAccountBelongsToSpecificClient() {
        assertEquals(clientId, account.getClientId());
    }

    @Test
    void testAccountCannotBeMixedWithOtherClientsAccounts() {
        Account otherClientAccount = new Account(102, "Trading", 2);
        assertNotEquals(account.getClientId(), otherClientAccount.getClientId());
    }

    // BR-09: Atomic updates of holdings and cash
    @Test
    void testExecuteTradeUpdatesHoldingsAndCashAtomically() throws Exception {
        account.depositCash(50000.0);
        Instrument apple = new Instrument(1, "Apple", "AAPL");

        // Execute buy trade atomically
        boolean success = account.executeTrade(apple, 100, 150.0, TradeType.BUY);

        assertTrue(success);
        assertEquals(100, account.getHoldingQuantity(apple.getInstrumentId()));
        assertEquals(35000.0, account.getCashBalance()); // 50000 - (100 * 150)
    }

    @Test
    void testTradeFailureDoesNotUpdateAnyState() {
        account.depositCash(10000.0);
        Instrument tesla = new Instrument(2, "Tesla", "TSLA");

        // Try to buy more than we can afford
        boolean success = account.executeTrade(tesla, 1000, 200.0, TradeType.BUY);

        assertFalse(success);
        assertEquals(0, account.getHoldingQuantity(tesla.getInstrumentId()));
        assertEquals(10000.0, account.getCashBalance()); // Unchanged
    }
}