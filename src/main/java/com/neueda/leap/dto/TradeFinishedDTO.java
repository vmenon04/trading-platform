package com.neueda.leap.dto;

import com.neueda.leap.enums.RejectionCode;
import com.neueda.leap.enums.TradeSide;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Message published to trade.finished with the outcome of a trade.
 *
 * @param tradeId identifier of the trade
 * @param accountId account that placed the trade
 * @param instrumentId instrument being traded
 * @param side BUY or SELL
 * @param quantity quantity traded
 * @param price unit execution price, or null if the trade was rejected
 * @param reason why the trade was rejected, for staff (it has internal ids and amounts), or null if it was fulfilled
 * @param externalTradeId the trade's public identifier
 * @param rejectionCode why the trade was rejected, safe to show the user, or null if it was fulfilled
 */
public record TradeFinishedDTO(
        Long tradeId,
        Long accountId,
        Long instrumentId,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal price,
        String reason,
        UUID externalTradeId,
        RejectionCode rejectionCode
) {
}
