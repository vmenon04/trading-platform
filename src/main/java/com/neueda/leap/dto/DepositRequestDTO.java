package com.neueda.leap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequestDTO(
        @NotNull UUID accountId,
        @NotNull UUID clientId,
        @NotNull @Positive BigDecimal amount
) {}