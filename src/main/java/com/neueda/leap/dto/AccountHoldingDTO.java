package com.neueda.leap.dto;

public class AccountHoldingDTO {
    private int accountId;
    private int instrumentId;
    private String asOfDate;
    private int quantity;
    private String status;

    public int getAccountId() {
        return accountId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getAsOfDate() {
        return asOfDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus() {
        this.status = status;
    }
}
