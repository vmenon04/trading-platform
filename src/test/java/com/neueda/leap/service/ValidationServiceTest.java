package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.entity.Instrument;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private static final int ACCOUNT_ID = 1;
    private static final int INSTRUMENT_ID = 10;
    private static final BigDecimal PRICE = new BigDecimal("100");

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountHoldingService accountHoldingService;

    @InjectMocks
    private ValidationService validationService;

    private static OrderRequestDTO order(String side, String quantity) {
        return new OrderRequestDTO(ACCOUNT_ID, INSTRUMENT_ID, side, new BigDecimal(quantity));
    }

    private void givenInstrumentAndBalance(String balance) {
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(new Instrument("Apple Inc", "AAPL", Instrument.InstrumentType.EQUITY));
        when(accountService.getBalance(ACCOUNT_ID)).thenReturn(new BigDecimal(balance));
    }

    @Test
    void validBuyPasses() {
        givenInstrumentAndBalance("1000");
        assertDoesNotThrow(() -> validationService.validate(order("BUY", "5"), PRICE));
    }

    @Test
    void buyWithExactCashPasses() {
        givenInstrumentAndBalance("1000");
        assertDoesNotThrow(() -> validationService.validate(order("BUY", "10"), PRICE));
    }

    @Test
    void buyRejectsInsufficientCash() {
        givenInstrumentAndBalance("1000");
        assertThrows(IllegalStateException.class, () -> validationService.validate(order("BUY", "11"), PRICE));
    }

    @Test
    void validSellPasses() {
        givenInstrumentAndBalance("1000");
        when(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("10"));
        assertDoesNotThrow(() -> validationService.validate(order("SELL", "5"), PRICE));
    }

    @Test
    void sellRejectsInsufficientHoldings() {
        givenInstrumentAndBalance("1000");
        when(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("10"));
        assertThrows(IllegalStateException.class, () -> validationService.validate(order("SELL", "11"), PRICE));
    }

    @Test
    void rejectsUnknownSide() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("HOLD", "5"), PRICE));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("BUY", "0"), PRICE));
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("BUY", "-5"), PRICE));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(null, PRICE));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsUnknownInstrument() {
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenThrow(new NoSuchElementException());
        assertThrows(NoSuchElementException.class, () -> validationService.validate(order("BUY", "5"), PRICE));
    }

    @Test
    void rejectsUnknownAccount() {
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenReturn(new Instrument("Apple Inc", "AAPL", Instrument.InstrumentType.EQUITY));
        when(accountService.getBalance(ACCOUNT_ID)).thenThrow(new NoSuchElementException());
        assertThrows(NoSuchElementException.class, () -> validationService.validate(order("BUY", "5"), PRICE));
    }
}
