package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    private static final int ACCOUNT_ID = 1;
    private static final int INSTRUMENT_ID = 10;
    private static final BigDecimal PRICE = new BigDecimal("100");

    @Mock
    private AccountService accountService;

    @Mock
    private AccountHoldingService accountHoldingService;

    @InjectMocks
    private ExecutionService executionService;

    // Compares by value, ignoring scale (so 500 matches 500.0000)
    private static BigDecimal amountEqualTo(String expected) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(expected)) == 0);
    }

    private static OrderRequest order(String side, String quantity) {
        return new OrderRequest(ACCOUNT_ID, INSTRUMENT_ID, side, new BigDecimal(quantity));
    }

    @Test
    void buyWithdrawsCostThenAddsHoldings() {
        executionService.execute(order("BUY", "5"), PRICE);

        InOrder inOrder = inOrder(accountService, accountHoldingService);
        inOrder.verify(accountService).withdraw(eq(ACCOUNT_ID), amountEqualTo("500"));
        inOrder.verify(accountHoldingService).addQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        verifyNoMoreInteractions(accountService, accountHoldingService);
    }

    @Test
    void sellRemovesHoldingsThenDepositsProceeds() {
        executionService.execute(order("SELL", "5"), PRICE);

        InOrder inOrder = inOrder(accountHoldingService, accountService);
        inOrder.verify(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        inOrder.verify(accountService).deposit(eq(ACCOUNT_ID), amountEqualTo("500"));
        verifyNoMoreInteractions(accountService, accountHoldingService);
    }

    @Test
    void buyDoesNotAddHoldingsWhenWithdrawFails() {
        doThrow(new IllegalStateException("Insufficient funds"))
                .when(accountService).withdraw(eq(ACCOUNT_ID), any());

        assertThrows(IllegalStateException.class, () -> executionService.execute(order("BUY", "5"), PRICE));
        verifyNoInteractions(accountHoldingService);
    }

    @Test
    void sellDoesNotDepositWhenRemoveFails() {
        doThrow(new IllegalStateException("Insufficient quantity"))
                .when(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), any());

        assertThrows(IllegalStateException.class, () -> executionService.execute(order("SELL", "5"), PRICE));
        verifyNoInteractions(accountService);
    }

    @Test
    void rejectsUnknownSide() {
        assertThrows(IllegalArgumentException.class, () -> executionService.execute(order("HOLD", "5"), PRICE));
        verifyNoInteractions(accountService, accountHoldingService);
    }
}
