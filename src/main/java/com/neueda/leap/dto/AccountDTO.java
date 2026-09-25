package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AccountDTO(
        @NotNull @Positive Integer accountId,
        @NotBlank String accountType
) {

    public Integer getAccountId() {
        return accountId;
    }

    public String getAccountType() {
        return accountType;
    }
}
