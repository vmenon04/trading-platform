package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TradeSubmittedDTO
   (@Positive int accountId,
    @Positive int instrumentId,
    @NotBlank String side,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal quote,
    @NotNull long task_id) {

    public int getAccountId() {
        return accountId;
    }
}


