package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountTradeDTO(
        @Positive Long tradeId,
        @NotBlank String tradeTime,
        @Positive Long accountId,
        @Positive Long instrumentId,
        @NotBlank String tradeType,
        @Positive BigDecimal quantity,
        @Positive BigDecimal price,
        @NotBlank String status
) {}
