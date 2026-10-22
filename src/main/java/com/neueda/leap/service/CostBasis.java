package com.neueda.leap.service;

import com.neueda.leap.entity.AccountTrade;
import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What an account paid for the shares it still holds, by the average cost method: a buy adds its total price to
 * the instrument's cost, and a sell removes the sold shares' share of that cost.
 */
public final class CostBasis {

    private CostBasis() {
    }

    /**
     * @param trades the account's trades, oldest first; only FULFILLED trades with a price count, so holdings with
     *               no priced trades (e.g. sample data) add no cost
     * @return the cost basis of the shares still held, to the cent
     */
    public static BigDecimal of(List<AccountTrade> trades) {
        Map<Long, Position> positions = new HashMap<>();
        for (AccountTrade trade : trades) {
            if (trade.getStatus() != TradeStatus.FULFILLED || trade.getTotalPrice() == null) {
                continue;
            }
            Position position = positions.computeIfAbsent(trade.getInstrumentId(), id -> new Position());
            if (trade.getTradeSide() == TradeSide.BUY) {
                position.buy(trade.getQuantity(), trade.getTotalPrice());
            } else {
                position.sell(trade.getQuantity());
            }
        }
        return positions.values().stream()
                .map(position -> position.cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static final class Position {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;

        void buy(BigDecimal shares, BigDecimal totalPrice) {
            quantity = quantity.add(shares);
            cost = cost.add(totalPrice);
        }

        void sell(BigDecimal shares) {
            if (quantity.signum() <= 0) {
                return;
            }
            if (shares.compareTo(quantity) >= 0) {
                quantity = BigDecimal.ZERO;
                cost = BigDecimal.ZERO;
                return;
            }
            // the sold shares take their average cost with them
            BigDecimal soldCost = cost.multiply(shares).divide(quantity, 10, RoundingMode.HALF_UP);
            cost = cost.subtract(soldCost);
            quantity = quantity.subtract(shares);
        }
    }
}
