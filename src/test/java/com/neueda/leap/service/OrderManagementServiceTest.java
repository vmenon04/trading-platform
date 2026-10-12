package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.repository.AccountTradeMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
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
class OrderManagementServiceTest {

    private static final int ACCOUNT_ID = 1;
    private static final int INSTRUMENT_ID = 10;
    private static final int TRADE_ID = 42;
    private static final BigDecimal QUANTITY = new BigDecimal("5");
    private static final BigDecimal PRICE = new BigDecimal("100");
    private static final OrderRequestDTO ORDER = new OrderRequestDTO(ACCOUNT_ID, INSTRUMENT_ID, "BUY", QUANTITY);

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private ValidationService validationService;

    @Mock
    private ExecutionService executionService;

    @Mock
    private AccountTradeMapper accountTradeMapper;

    @InjectMocks
    private OrderManagementService orderManagementService;

    private void givenPrice(BigDecimal price) {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(price);
    }

    private void givenPendingTradeRecorded(BigDecimal price) {
        when(accountTradeMapper.insertTrade(ACCOUNT_ID, INSTRUMENT_ID, "BUY", QUANTITY, price, "PENDING"))
                .thenReturn(TRADE_ID);
    }

    @Test
    void successfulOrderRecordsPendingAcceptsExecutesThenFulfills() {
        givenPrice(PRICE);
        givenPendingTradeRecorded(PRICE);

        assertEquals(TRADE_ID, orderManagementService.placeOrder(ORDER));

        InOrder inOrder = inOrder(validationService, accountTradeMapper, executionService);
        inOrder.verify(validationService).validate(ORDER, PRICE);
        inOrder.verify(accountTradeMapper).insertTrade(ACCOUNT_ID, INSTRUMENT_ID, "BUY", QUANTITY, PRICE, "PENDING");
        inOrder.verify(accountTradeMapper).insertStatus(TRADE_ID, "ACCEPTED");
        inOrder.verify(executionService).execute(ORDER, PRICE);
        inOrder.verify(accountTradeMapper).insertStatus(TRADE_ID, "FULFILLED");
    }

    @Test
    void insufficientFundsRecordsRejectedTradeAndThrows() {
        givenPrice(PRICE);
        doThrow(new IllegalStateException("Insufficient funds")).when(validationService).validate(ORDER, PRICE);

        assertThrows(IllegalStateException.class, () -> orderManagementService.placeOrder(ORDER));

        verify(accountTradeMapper).insertTrade(ACCOUNT_ID, INSTRUMENT_ID, "BUY", QUANTITY, PRICE, "REJECTED");
        verify(accountTradeMapper, never()).insertStatus(anyInt(), any());
        verifyNoInteractions(executionService);
    }

    @Test
    void malformedOrderThrowsWithoutRecording() {
        givenPrice(PRICE);
        doThrow(new IllegalArgumentException("Quantity must be positive"))
                .when(validationService).validate(ORDER, PRICE);

        assertThrows(IllegalArgumentException.class, () -> orderManagementService.placeOrder(ORDER));

        verifyNoInteractions(accountTradeMapper, executionService);
    }

    @Test
    void unknownInstrumentThrowsWithoutRecording() {
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenThrow(new NoSuchElementException());

        assertThrows(NoSuchElementException.class, () -> orderManagementService.placeOrder(ORDER));

        verifyNoInteractions(validationService, accountTradeMapper, executionService);
    }

    @Test
    void executionFailureMarksTradeRejected() {
        givenPrice(PRICE);
        givenPendingTradeRecorded(PRICE);
        doThrow(new IllegalStateException("Insufficient funds")).when(executionService).execute(ORDER, PRICE);

        assertThrows(IllegalStateException.class, () -> orderManagementService.placeOrder(ORDER));

        verify(accountTradeMapper).insertStatus(TRADE_ID, "REJECTED");
        verify(accountTradeMapper, never()).insertStatus(TRADE_ID, "FULFILLED");
    }

    @Test
    void validatesAndExecutesWithFetchedPrice() {
        BigDecimal fetchedPrice = new BigDecimal("123.45");
        givenPrice(fetchedPrice);
        givenPendingTradeRecorded(fetchedPrice);

        orderManagementService.placeOrder(ORDER);

        verify(validationService).validate(ORDER, fetchedPrice);
        verify(executionService).execute(ORDER, fetchedPrice);
    }
}
