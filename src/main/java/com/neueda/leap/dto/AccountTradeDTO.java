package com.neueda.leap.dto;

import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountTradeDTO(
        @Positive Long tradeId,
        @NotBlank String tradeTime,
        @Positive Long accountId,
        @Positive Long instrumentId,
        @NotBlank TradeSide tradeType,
        @Positive BigDecimal quantity,
        @Positive BigDecimal price,
        @NotBlank TradeStatus status
) {}
