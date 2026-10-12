package com.neueda.leap.dto;

import com.neueda.leap.enums.ActivityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AccountSubscriptionDTO(
        @Positive Long accountId,
        @Positive Long modelPortfolioId,
        @NotBlank String subscriptionDate,
        @NotNull ActivityStatus status
) {}
