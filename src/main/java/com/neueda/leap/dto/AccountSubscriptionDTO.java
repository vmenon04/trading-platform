package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AccountSubscriptionDTO(
        @Positive UUID accountId,
        @Positive int modelPortfolioId,
        @NotBlank String subscriptionDate,
        @NotBlank String status
) {}
