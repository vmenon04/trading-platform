package com.neueda.leap.dto;

import java.math.BigDecimal;

/**
 * Message published to trade.recorded once a validated trade has been saved as PENDING.
 *
 * @param tradeId identifier of the recorded trade
 * @param accountId account placing the trade
 * @param instrumentId instrument being traded
 * @param side BUY or SELL
 * @param quantity quantity to trade
 */
public record TradeRecordedDTO(
        Long tradeId,
        Long accountId,
        Long instrumentId,
        String side,
        BigDecimal quantity
) {
}
