package com.neueda.leap.dto;

import java.math.BigDecimal;

/**
 * Message published to trade.finished with the outcome of a trade.
 *
 * @param tradeId identifier of the trade
 * @param accountId account that placed the trade
 * @param instrumentId instrument being traded
 * @param side BUY or SELL
 * @param quantity quantity traded
 * @param price unit execution price, or null if the trade was rejected
 */
public record TradeFinishedDTO(
        Long tradeId,
        Long accountId,
        Long instrumentId,
        String side,
        BigDecimal quantity,
        BigDecimal price
) {
}
