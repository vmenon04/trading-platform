package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderResponseDTO(
        @Positive int tradeId,
        @NotBlank String status,
        @Positive BigDecimal executedPrice,
        @Positive BigDecimal executedQuantity,
        @Size(min=0) String reason
) {
}
