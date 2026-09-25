package com.neueda.leap.entity;

import java.time.LocalDate;

public class AccountTrade {

    public enum TradeType {
        BUY, SELL
    }

    public enum TradeStatus {
        PENDING, ACCEPTED, REJECTED, FULFILLED
    }

    private Long tradeId;
    private LocalDate tradeTime;
    private int accountId;
    private int instrumentId;
    private TradeType tradeType;
    private double quantity;
    private double price;
    private TradeStatus tradeStatus;

    public AccountTrade() {
    }

    public AccountTrade(int accountId, LocalDate tradeTime, int instrumentId, TradeType tradeType, double quantity, double price) {
        this.tradeId = null;
        this.accountId = accountId;
        this.tradeTime = tradeTime;
        this.instrumentId = instrumentId;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradeStatus = TradeStatus.PENDING;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(int instrumentId) {
        this.instrumentId = instrumentId;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public TradeStatus getStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public LocalDate getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(LocalDate tradeTime) {
        this.tradeTime = tradeTime;
    }
}
