package com.neueda.leap.entity;

import java.time.LocalDate;

public class AccountTrade {

    public enum TradeType {
        BUY, SELL
    };

    public enum TradeStatus {
        PENDING, ACCEPTED, REJECTED, FULFILLED
    };

    private final Long tradeId;
    private final LocalDate createdTime;
    private LocalDate processTime;
    private LocalDate fulfilledTime;
    private final int accountId;
    private final int instrumentId;
    private final TradeType tradeType;
    private final double quantity;
    private final double price;
    private final TradeStatus tradeStatus;

    public AccountTrade(int accountId, LocalDate createdTime, int instrumentId, TradeType tradeType, double quantity, double price) {
        this.tradeId = null;

        if(quantity <= 0) {
            throw new IllegalArgumentException("Trade quantity must be positive.");
        }

        if(price <= 0) {
            throw new IllegalArgumentException("Trade price must be positive.");
        }

        this.accountId = accountId;
        this.createdTime = createdTime;
        this.instrumentId = instrumentId;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradeStatus = TradeStatus.PENDING;
    }

    public Long getTradeId() {
        return tradeId;
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

    public LocalDate getCreationTime() {
        return createdTime;
    }
}
