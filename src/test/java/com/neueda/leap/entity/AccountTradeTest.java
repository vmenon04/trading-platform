package com.neueda.leap.entity;

import com.neueda.leap.entity.AccountTrade.TradeStatus;
import com.neueda.leap.entity.AccountTrade.TradeType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountTradeTest {

    @Test
    void noArgsConstructorLeavesFieldsAtDefaults() {
        AccountTrade trade = new AccountTrade();

        assertAll(
                () -> assertNull(trade.getTradeId()),
                () -> assertEquals(0, trade.getAccountId()),
                () -> assertNull(trade.getTradeTime()),
                () -> assertEquals(0, trade.getInstrumentId()),
                () -> assertNull(trade.getTradeType()),
                () -> assertEquals(0.0, trade.getQuantity()),
                () -> assertEquals(0.0, trade.getPrice()),
                () -> assertNull(trade.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValuesAndDefaultsStatusToPending() {
        LocalDate tradeTime = LocalDate.of(2026, 9, 25);
        AccountTrade trade = new AccountTrade(101, tradeTime, 7, TradeType.BUY, 10.5, 55.25);

        assertAll(
                () -> assertNull(trade.getTradeId()),
                () -> assertEquals(101, trade.getAccountId()),
                () -> assertEquals(tradeTime, trade.getTradeTime()),
                () -> assertEquals(7, trade.getInstrumentId()),
                () -> assertEquals(TradeType.BUY, trade.getTradeType()),
                () -> assertEquals(10.5, trade.getQuantity()),
                () -> assertEquals(55.25, trade.getPrice()),
                () -> assertEquals(TradeStatus.PENDING, trade.getStatus())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        AccountTrade trade = new AccountTrade();
        LocalDate tradeTime = LocalDate.of(2025, 5, 10);

        trade.setTradeId(1L);
        trade.setAccountId(2);
        trade.setTradeTime(tradeTime);
        trade.setInstrumentId(3);
        trade.setTradeType(TradeType.SELL);
        trade.setQuantity(4.5);
        trade.setPrice(6.75);
        trade.setTradeStatus(TradeStatus.ACCEPTED);

        assertAll(
                () -> assertEquals(1L, trade.getTradeId()),
                () -> assertEquals(2, trade.getAccountId()),
                () -> assertEquals(tradeTime, trade.getTradeTime()),
                () -> assertEquals(3, trade.getInstrumentId()),
                () -> assertEquals(TradeType.SELL, trade.getTradeType()),
                () -> assertEquals(4.5, trade.getQuantity()),
                () -> assertEquals(6.75, trade.getPrice()),
                () -> assertEquals(TradeStatus.ACCEPTED, trade.getStatus())
        );
    }
}