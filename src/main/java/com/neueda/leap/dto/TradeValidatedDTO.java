package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeValidatedDTO(

        @Positive Long instrumentId,
        @Positive UUID accountId,
        @NotNull String side,
        @Positive BigDecimal quantity,
        @Positive BigDecimal quote,
        @NotBlank long taskId
){}
