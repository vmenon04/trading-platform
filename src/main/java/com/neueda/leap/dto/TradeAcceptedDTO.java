package com.neueda.leap.dto;

import com.neueda.leap.enums.TradeSide;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TradeAcceptedDTO(
        @NotNull @Positive Long tradeId,
        @NotNull @Positive Long accountId,
        @NotNull @Positive Long instrumentId,
        @NotNull TradeSide side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price
) {}
