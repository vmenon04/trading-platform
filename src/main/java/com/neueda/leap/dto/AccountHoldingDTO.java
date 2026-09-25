package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountHoldingDTO(
        @Positive int accountId,
        @Positive int instrumentId,
        @NotBlank String asOfDate,
        @Positive BigDecimal quantity,
        @NotBlank String status
) {

    public int getAccountId() {
        return accountId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getAsOfDate() {
        return asOfDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }
}
