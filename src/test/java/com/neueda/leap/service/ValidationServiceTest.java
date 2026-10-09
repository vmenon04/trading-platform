package com.neueda.leap.service;

import com.neueda.leap.dto.TradeSubmittedDTO;
import com.neueda.leap.dto.TradeValidatedDTO;
import com.neueda.leap.entity.Instrument;
import com.neueda.leap.enums.InstrumentType;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    private static final UUID EXTERNAL_ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long ACCOUNT_ID = 1L;
    private static final Long INSTRUMENT_ID = 10L;
    private static final BigDecimal PRICE = new BigDecimal("100");
    private static final long TASK_ID = 77L;
    private static final String BALANCE = "1000";
    private static final String TICKER = "AAPL";

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private AccountService accountService;

    @Mock
    private AccountHoldingService accountHoldingService;

    @InjectMocks
    private ValidationService validationService;

    private static TradeSubmittedDTO order(String side, String quantity) {
        return new TradeSubmittedDTO(
                INSTRUMENT_ID,
                EXTERNAL_ACCOUNT_ID,
                TradeSide.valueOf(side),
                new BigDecimal(quantity),
                TASK_ID
        );
    }

    private void givenInstrumentAndBalance() {
        when(instrumentService.getInstrumentById(INSTRUMENT_ID))
                .thenReturn(new Instrument("Apple Inc", TICKER, InstrumentType.STOCK));
        when(instrumentService.getCurrentPrice(INSTRUMENT_ID)).thenReturn(PRICE);
        when(accountService.getAccountIdByExternalAccountId(EXTERNAL_ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
        when(accountService.getBalance(ACCOUNT_ID)).thenReturn(new BigDecimal(BALANCE));
    }

    @Test
    void validBuyPasses() {
        givenInstrumentAndBalance();
        assertDoesNotThrow(() -> validationService.validate(order("BUY", "5")));
    }

    @Test
    void buyWithExactCashPasses() {
        givenInstrumentAndBalance();
        assertDoesNotThrow(() -> validationService.validate(order("BUY", "10")));
    }

    @Test
    void buyRejectsInsufficientCash() {
        givenInstrumentAndBalance();
        assertThrows(IllegalStateException.class, () -> validationService.validate(order("BUY", "11")));
    }

    @Test
    void validSellPasses() {
        givenInstrumentAndBalance();
        when(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("10"));
        assertDoesNotThrow(() -> validationService.validate(order("SELL", "5")));
    }

    @Test
    void sellRejectsInsufficientHoldings() {
        givenInstrumentAndBalance();
        when(accountHoldingService.getQuantity(ACCOUNT_ID, INSTRUMENT_ID)).thenReturn(new BigDecimal("10"));
        assertThrows(IllegalStateException.class, () -> validationService.validate(order("SELL", "11")));
    }

    @Test
    void rejectsUnknownSide() {
        TradeSubmittedDTO invalidOrder = new TradeSubmittedDTO(
                INSTRUMENT_ID,
                EXTERNAL_ACCOUNT_ID,
                null,
                new BigDecimal("5"),
                TASK_ID
        );

        assertThrows(IllegalArgumentException.class, () -> validationService.validate(invalidOrder));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("BUY", "0")));
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("BUY", "-5")));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsQuantityWithMoreThanEightDecimals() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(order("BUY", "0.000000001")));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void acceptsQuantityWithEightDecimals() {
        givenInstrumentAndBalance();
        assertDoesNotThrow(() -> validationService.validate(order("BUY", "0.12345678")));
    }

    @Test
    void rejectsNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> validationService.validate(null));
        verifyNoInteractions(instrumentService, accountService, accountHoldingService);
    }

    @Test
    void rejectsUnknownInstrument() {
        when(accountService.getAccountIdByExternalAccountId(EXTERNAL_ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
        when(instrumentService.getInstrumentById(INSTRUMENT_ID)).thenThrow(new NoSuchElementException());
        assertThrows(NoSuchElementException.class, () -> validationService.validate(order("BUY", "5")));
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountService.getAccountIdByExternalAccountId(EXTERNAL_ACCOUNT_ID)).thenThrow(new NoSuchElementException());
        assertThrows(NoSuchElementException.class, () -> validationService.validate(order("BUY", "5")));
    }

    @Test
    void validateSubmittedTradeReturnsValidatedDtoWithInternalAccountId() {
        givenInstrumentAndBalance();

        TradeValidatedDTO validatedDTO = validationService.validateSubmittedTrade(order("BUY", "5"));

        assertAll(
                () -> assertEquals(INSTRUMENT_ID, validatedDTO.instrumentId()),
                () -> assertEquals(ACCOUNT_ID, validatedDTO.accountId()),
                () -> assertEquals(TradeSide.BUY, validatedDTO.side()),
                () -> assertEquals(new BigDecimal("5"), validatedDTO.quantity()),
                () -> assertEquals(PRICE, validatedDTO.quote()),
                () -> assertEquals(TASK_ID, validatedDTO.taskId())
        );
    }
}
