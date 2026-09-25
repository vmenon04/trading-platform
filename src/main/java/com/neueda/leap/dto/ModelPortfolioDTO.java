package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ModelPortfolioDTO(
        @Positive int modelPortfolioId,
        @NotBlank String name
) {

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public String getName() {
        return name;
    }
}
