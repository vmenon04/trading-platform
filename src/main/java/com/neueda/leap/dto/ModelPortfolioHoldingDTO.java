package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ModelPortfolioHoldingDTO(
        @Positive int modelPortfolioId,
        @Positive int instrumentId,
        @NotBlank String effectiveDate,
        @PositiveOrZero double targetWeightPct,
        @NotBlank String status
) {}
