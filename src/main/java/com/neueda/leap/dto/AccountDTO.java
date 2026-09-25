package com.neueda.leap.dto;

import com.neueda.leap.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AccountDTO(
        @NotNull @Positive Integer accountId,
        @NotNull AccountType accountType
) {

    public Integer getAccountId() {
        return accountId;
    }

    public AccountType getAccountType() {
        return accountType;
    }
}
