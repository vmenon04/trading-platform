package com.neueda.leap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TradeValidatedDTO(

        @Positive Long instrumentId,
        @Positive Long accountId,
        @NotNull String side,
        @Positive BigDecimal quantity,
        @Positive BigDecimal quote

){}
