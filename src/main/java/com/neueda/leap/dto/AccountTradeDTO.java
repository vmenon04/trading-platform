package com.neueda.leap.dto;

public class AccountTradeDTO {
    private int tradeId;
    private String tradeTime;
    private int accountId;
    private int instrumentId;
    private String tradeType;
    private int quantity;
    private int price;
    private String status;

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

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus() {
        this.status = status;
    }
}
