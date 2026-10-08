package com.neueda.leap.dto;

import com.neueda.leap.enums.TradeSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TradeRequestDTO(
        @NotNull UUID accountId,
        @Positive Long instrumentId,
        @NotNull TradeSide side,
        @NotNull @Positive BigDecimal quantity
) {}
