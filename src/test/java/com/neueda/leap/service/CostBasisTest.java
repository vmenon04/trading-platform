package com.neueda.leap.service;

import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostBasisTest {

    private static final Long AAPL = 1L;
    private static final Long MSFT = 2L;

    @Test
    void noTrades_isZero() {
        assertEquals(new BigDecimal("0.00"), CostBasis.of(List.of()));
    }

    @Test
    void buys_addTheirTotalPrice() {
        assertEquals(new BigDecimal("1300.00"), CostBasis.of(List.of(
                fulfilled(AAPL, TradeSide.BUY, "2", "200"),
                fulfilled(AAPL, TradeSide.BUY, "2", "300"),
                fulfilled(MSFT, TradeSide.BUY, "1", "800"))));
    }

    @Test
    void sell_removesTheSoldSharesAtAverageCost() {
        // 4 shares cost 500 (average 125); selling 1 leaves 3 at 125 = 375, whatever it sold for
        assertEquals(new BigDecimal("375.00"), CostBasis.of(List.of(
                fulfilled(AAPL, TradeSide.BUY, "2", "200"),
                fulfilled(AAPL, TradeSide.BUY, "2", "300"),
                fulfilled(AAPL, TradeSide.SELL, "1", "999"))));
    }

    @Test
    void sellingEverything_leavesNoCost() {
        assertEquals(new BigDecimal("0.00"), CostBasis.of(List.of(
                fulfilled(AAPL, TradeSide.BUY, "3", "150"),
                fulfilled(AAPL, TradeSide.SELL, "3", "180"))));
    }

    @Test
    void pendingRejectedAndUnpricedTrades_areIgnored() {
        assertEquals(new BigDecimal("100.00"), CostBasis.of(List.of(
                trade(AAPL, TradeSide.BUY, "5", "500", TradeStatus.PENDING),
                trade(AAPL, TradeSide.BUY, "5", "500", TradeStatus.REJECTED),
                trade(AAPL, TradeSide.BUY, "5", null, TradeStatus.FULFILLED),
                fulfilled(AAPL, TradeSide.BUY, "1", "100"))));
    }

    private static AccountTrade fulfilled(Long instrumentId, TradeSide side, String quantity, String totalPrice) {
        return trade(instrumentId, side, quantity, totalPrice, TradeStatus.FULFILLED);
    }

    private static AccountTrade trade(Long instrumentId, TradeSide side, String quantity, String totalPrice,
                                      TradeStatus status) {
        AccountTrade trade = new AccountTrade(1L, instrumentId, side, new BigDecimal(quantity), null,
                totalPrice == null ? null : new BigDecimal(totalPrice));
        trade.setTradeStatus(status);
        return trade;
    }
}
