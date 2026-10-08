package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AccountSubscriptionDTO(
        @Positive Long accountId,
        @Positive Long modelPortfolioId,
        @NotBlank String subscriptionDate,
        @NotBlank String status
) {}
