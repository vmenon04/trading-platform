package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AccountSubscriptionDTO(
        @Positive int accountId,
        @Positive int modelPortfolioId,
        @NotBlank String subscriptionDate,
        @NotBlank String status
) {

    public int getAccountId() {
        return accountId;
    }

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public String getSubscriptionDate() {
        return subscriptionDate;
    }

    public String getStatus() {
        return status;
    }
}
