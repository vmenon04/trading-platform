package com.neueda.leap.dto;

import com.neueda.leap.enums.TradeSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TradeValidatedDTO(

        @Positive Long instrumentId,
        @Positive Long accountId,
        @NotNull TradeSide side,
        @Positive BigDecimal quantity,
        @Positive BigDecimal quote,
        @NotBlank Long taskId
){}
