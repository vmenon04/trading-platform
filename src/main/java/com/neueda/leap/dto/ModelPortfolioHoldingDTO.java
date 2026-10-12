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
) {

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public double getTargetWeightPct() {
        return targetWeightPct;
    }

    public String getStatus() {
        return status;
    }
}
