package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderResponseDTO(
        @Positive int accountId,
        @Positive int instrumentId,
        @NotBlank String status,
        @Positive BigDecimal fee,
        @NotBlank String reason
) {
}
