package com.neueda.leap.dto;

import com.neueda.leap.enums.TradeSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TradeRequestDTO(
        @Positive UUID accountId,
        @Positive int instrumentId,
        @NotBlank TradeSide side,
        @NotNull @Positive BigDecimal quantity
) {}
