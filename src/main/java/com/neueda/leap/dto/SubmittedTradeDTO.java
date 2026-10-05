package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmittedTradeDTO (
        @Positive long instrumentId,
        @Positive UUID accountId,
        @NotBlank String side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal quote,
        @NotBlank long taskId
) {}
