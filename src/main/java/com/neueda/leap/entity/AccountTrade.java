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
    private final LocalDate tradeTime;
    private final int accountId;
    private final int instrumentId;
    private final TradeType tradeType;
    private final double quantity;
    private final double price;
    private final TradeStatus tradeStatus;

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

    public int getInstrumentId() {
        return instrumentId;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public TradeStatus getStatus() {
        return tradeStatus;
    }

    public LocalDate getTradeTime() {
        return tradeTime;
    }
}
