package com.neueda.leap.entity;

import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.enums.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
                () -> assertNull(trade.getQuantity()),
                () -> assertNull(trade.getPrice()),
                () -> assertNull(trade.getStatus())
        );
    }

    @Test
    void parameterizedConstructorSetsProvidedValuesAndDefaultsStatusToPending() {
        OffsetDateTime tradeTime = OffsetDateTime.now();
        AccountTrade trade = new AccountTrade(101, tradeTime, 7, TradeSide.BUY, new BigDecimal("10.5"), new BigDecimal("55.25"));

        assertAll(
                () -> assertNull(trade.getTradeId()),
                () -> assertEquals(101, trade.getAccountId()),
                () -> assertEquals(tradeTime, trade.getTradeTime()),
                () -> assertEquals(7, trade.getInstrumentId()),
                () -> assertEquals(TradeSide.BUY, trade.getTradeType()),
                () -> assertEquals(BigDecimal.valueOf(10.5), trade.getQuantity()),
                () -> assertEquals(BigDecimal.valueOf(55.25), trade.getPrice()),
                () -> assertEquals(TradeStatus.SUBMITTED, trade.getStatus())
        );
    }

    @Test
    void settersUpdateAllMutableFields() {
        AccountTrade trade = new AccountTrade();
        OffsetDateTime tradeTime = OffsetDateTime.now();

        trade.setTradeId(1L);
        trade.setAccountId(2);
        trade.setTradeTime(tradeTime);
        trade.setInstrumentId(3);
        trade.setTradeType(TradeSide.SELL);
        trade.setQuantity(BigDecimal.valueOf(4.5));
        trade.setPrice(BigDecimal.valueOf(6.75));
        trade.setTradeStatus(TradeStatus.ACCEPTED);

        assertAll(
                () -> assertEquals(1L, trade.getTradeId()),
                () -> assertEquals(2, trade.getAccountId()),
                () -> assertEquals(tradeTime, trade.getTradeTime()),
                () -> assertEquals(3, trade.getInstrumentId()),
                () -> assertEquals(TradeSide.SELL, trade.getTradeType()),
                () -> assertEquals(BigDecimal.valueOf(4.5), trade.getQuantity()),
                () -> assertEquals(BigDecimal.valueOf(6.75), trade.getPrice()),
                () -> assertEquals(TradeStatus.ACCEPTED, trade.getStatus())
        );
    }
}