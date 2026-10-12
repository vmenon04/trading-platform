package com.neueda.leap.service;

import com.neueda.leap.dto.TradeFinishedDTO;
import com.neueda.leap.dto.TradeRecordedDTO;
import com.neueda.leap.dto.TradeRequestDTO;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradePriceMapper;
import com.neueda.leap.repository.AccountTradeStatusMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final UUID EXTERNAL_ACCOUNT_ID = UUID.randomUUID();
    private static final Long INSTRUMENT_ID = 10L;
    private static final Long TRADE_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("100");

    @Mock
    private AccountService accountService;

    @Mock
    private AccountHoldingService accountHoldingService;

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private AccountTradeStatusMapper accountTradeStatusMapper;

    @Mock
    private AccountTradePriceMapper accountTradePriceMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        executionService = new ExecutionService(accountService, accountHoldingService, instrumentService,
                accountTradeStatusMapper, accountTradePriceMapper, new TransactionTemplate(transactionManager));
    }

    // Compares by value, ignoring scale (so 500 matches 500.0000)
    private static BigDecimal amountEqualTo(String expected) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(expected)) == 0);
    }

    private static TradeRequestDTO order(TradeSide side, String quantity) {
        return new TradeRequestDTO(EXTERNAL_ACCOUNT_ID, INSTRUMENT_ID, side, new BigDecimal(quantity));
    }

    private static TradeRecordedDTO recorded(TradeSide side, String quantity) {
        return new TradeRecordedDTO(INSTRUMENT_ID, ACCOUNT_ID, side, new BigDecimal(quantity),
                new BigDecimal("99"), 3L, TRADE_ID);
    }

    @Test
    void buyPurchasesThenAddsHoldings() {
        executionService.execute(order(TradeSide.BUY, "5"), PRICE);

        InOrder inOrder = inOrder(accountService, accountHoldingService);
        inOrder.verify(accountService).purchase(eq(ACCOUNT_ID), amountEqualTo("500"));
        inOrder.verify(accountHoldingService).addQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        verifyNoMoreInteractions(accountService, accountHoldingService);
    }

    @Test
    void sellRemovesHoldingsThenSellsForProceeds() {
        executionService.execute(order(TradeSide.SELL, "5"), PRICE);

        InOrder inOrder = inOrder(accountHoldingService, accountService);
        inOrder.verify(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        inOrder.verify(accountService).sell(eq(ACCOUNT_ID), amountEqualTo("500"));
        verifyNoMoreInteractions(accountService, accountHoldingService);
    }

    @Test
    void buyDoesNotAddHoldingsWhenPurchaseFails() {
        doThrow(new IllegalStateException("Insufficient funds"))
                .when(accountService).purchase(eq(ACCOUNT_ID), any());

        assertThrows(IllegalStateException.class, () -> executionService.execute(order(TradeSide.BUY, "5"), PRICE));
        verifyNoInteractions(accountHoldingService);
    }

    @Test
    void sellDoesNotCreditWhenRemoveFails() {
        doThrow(new IllegalStateException("Insufficient quantity"))
                .when(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), any());

        assertThrows(IllegalStateException.class, () -> executionService.execute(order(TradeSide.SELL, "5"), PRICE));
        verifyNoInteractions(accountService);
    }

    @Test
    void recordedBuyIsFulfilledAtTheCurrentPrice() {
        when(accountTradeStatusMapper.getTradeStatusByTradeId(TRADE_ID)).thenReturn(TradeStatus.PENDING);
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        InOrder inOrder = inOrder(accountService, accountHoldingService, accountTradePriceMapper,
                accountTradeStatusMapper, transactionManager);
        inOrder.verify(accountService).purchase(eq(ACCOUNT_ID), amountEqualTo("500"));
        inOrder.verify(accountHoldingService).addQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        inOrder.verify(accountTradePriceMapper).insertTradePrice(TRADE_ID, 100.0, 500.0);
        inOrder.verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.FULFILLED);
        inOrder.verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());

        assertEquals(TRADE_ID, outcome.tradeId());
        assertEquals(ACCOUNT_ID, outcome.accountId());
        assertEquals(INSTRUMENT_ID, outcome.instrumentId());
        assertEquals(TradeSide.BUY, outcome.side());
        assertEquals(0, outcome.quantity().compareTo(new BigDecimal("5")));
        assertEquals(0, outcome.price().compareTo(PRICE));
        assertNull(outcome.reason());
    }

    @Test
    void recordedSellIsFulfilledAtTheCurrentPrice() {
        when(accountTradeStatusMapper.getTradeStatusByTradeId(TRADE_ID)).thenReturn(TradeStatus.PENDING);
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.SELL, "5"));

        InOrder inOrder = inOrder(accountHoldingService, accountService);
        inOrder.verify(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), amountEqualTo("5"));
        inOrder.verify(accountService).sell(eq(ACCOUNT_ID), amountEqualTo("500"));
        verify(accountTradePriceMapper).insertTradePrice(TRADE_ID, 100.0, 500.0);
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.FULFILLED);
        verify(transactionManager).commit(any());
        assertEquals(0, outcome.price().compareTo(PRICE));
        assertNull(outcome.reason());
    }

    @Test
    void recordedBuyIsRejectedWhenFundsAreInsufficient() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);
        doThrow(new IllegalStateException("Insufficient funds"))
                .when(accountService).purchase(eq(ACCOUNT_ID), any());

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(accountHoldingService);
        verify(accountTradePriceMapper, never()).insertTradePrice(any(), any(), any());
        verify(accountTradeStatusMapper, never()).insertTradeStatus(TRADE_ID, TradeStatus.FULFILLED);
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.REJECTED);
        assertNull(outcome.price());
        assertEquals("Insufficient funds", outcome.reason());
    }

    @Test
    void recordedSellIsRejectedWhenHoldingsAreInsufficient() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);
        doThrow(new IllegalStateException("Insufficient quantity"))
                .when(accountHoldingService).removeQuantity(eq(ACCOUNT_ID), eq(INSTRUMENT_ID), any());

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.SELL, "5"));

        verifyNoInteractions(accountService);
        verify(transactionManager).rollback(any());
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.REJECTED);
        assertNull(outcome.price());
        assertEquals("Insufficient quantity", outcome.reason());
    }

    @Test
    void recordedTradeIsRejectedWhenNoPriceIsAvailable() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID))
                .thenThrow(new IllegalStateException("No price available for X:BTC-USD"));

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(accountService, accountHoldingService, accountTradePriceMapper, transactionManager);
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.REJECTED);
        assertNull(outcome.price());
        assertEquals("No price available for X:BTC-USD", outcome.reason());
    }

    @Test
    void recordedTradeIsRejectedWhenInstrumentIsUnknown() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID))
                .thenThrow(new NoSuchElementException("No instrument with id 10"));

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(accountService, accountHoldingService, transactionManager);
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.REJECTED);
        assertNull(outcome.price());
        assertEquals("No instrument with id 10", outcome.reason());
    }

    @Test
    void recordedTradeIsRejectedWhenPriceApiFails() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID))
                .thenThrow(new RestClientException("Fauxnance unavailable"));

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(accountService, accountHoldingService, transactionManager);
        verify(accountTradeStatusMapper).insertTradeStatus(TRADE_ID, TradeStatus.REJECTED);
        assertNull(outcome.price());
        assertEquals("Fauxnance unavailable", outcome.reason());
    }

    @Test
    void databaseErrorRollsBackAndPropagates() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);
        doThrow(new DataAccessResourceFailureException("Database down"))
                .when(accountTradePriceMapper).insertTradePrice(any(), any(), any());

        assertThrows(DataAccessResourceFailureException.class,
                () -> executionService.execute(recorded(TradeSide.BUY, "5")));

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(accountTradeStatusMapper, never()).insertTradeStatus(any(), any());
    }

    @Test
    void redeliveredFulfilledTradeReturnsStoredPrice() {
        when(accountTradeStatusMapper.getTradeStatusByTradeId(TRADE_ID)).thenReturn(TradeStatus.FULFILLED);
        when(accountTradePriceMapper.getTradePriceByTradeId(TRADE_ID)).thenReturn(100.0);

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(instrumentService, accountService, accountHoldingService, transactionManager);
        verify(accountTradeStatusMapper, never()).insertTradeStatus(any(), any());
        verify(accountTradePriceMapper, never()).insertTradePrice(any(), any(), any());
        assertEquals(TRADE_ID, outcome.tradeId());
        assertEquals(0, outcome.price().compareTo(PRICE));
    }

    @Test
    void redeliveredRejectedTradeReturnsNoPrice() {
        when(accountTradeStatusMapper.getTradeStatusByTradeId(TRADE_ID)).thenReturn(TradeStatus.REJECTED);
        // a rejected trade has no account_trade_price row
        when(accountTradePriceMapper.getTradePriceByTradeId(TRADE_ID)).thenReturn(null);

        TradeFinishedDTO outcome = executionService.execute(recorded(TradeSide.BUY, "5"));

        verifyNoInteractions(instrumentService, accountService, accountHoldingService, transactionManager);
        verify(accountTradeStatusMapper, never()).insertTradeStatus(any(), any());
        assertNull(outcome.price());
    }
}
