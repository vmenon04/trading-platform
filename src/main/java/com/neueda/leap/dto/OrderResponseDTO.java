package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderResponseDTO(
        @Positive int tradeId,
        @NotBlank String status,
        @Positive BigDecimal executedPrice,
        @Positive BigDecimal executedQuantity,
        @NotBlank String reason
) {
}
