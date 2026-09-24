package com.neueda.leap.dto;

import java.math.BigDecimal;

public class OrderRequestDTO {

    private int accountId;
    private int instrumentId;
    private String side;
    private BigDecimal quantity;

    public OrderRequestDTO(int accountId, int instrumentId, String side, BigDecimal quantity) {
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
    }

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
}
