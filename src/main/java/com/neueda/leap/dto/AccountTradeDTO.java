package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountTradeDTO(
        @Positive int tradeId,
        @NotBlank String tradeTime,
        @Positive int accountId,
        @Positive int instrumentId,
        @NotBlank String tradeType,
        @Positive BigDecimal quantity,
        @Positive BigDecimal price,
        @NotBlank String status
) {

    public int getTradeId() {
        return tradeId;
    }

    public String getTradeTime() {
        return tradeTime;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getTradeType() {
        return tradeType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}
