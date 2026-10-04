package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderRequestDTO(
        @Positive int accountId,
        @Positive int instrumentId,
        @NotBlank String side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal quote
) {

    public int getAccountId() {
        return accountId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getQuote() {
        return quote;
    }
}
