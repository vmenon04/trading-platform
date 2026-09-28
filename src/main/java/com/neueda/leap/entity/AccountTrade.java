package com.neueda.leap.entity;

import java.math.BigDecimal;

import java.time.OffsetDateTime;

public class AccountTrade {

    public enum TradeType {
        BUY, SELL
    }

    public enum TradeStatus {
        PENDING, ACCEPTED, REJECTED, FULFILLED
    }

    private Long tradeId;
    private OffsetDateTime tradeTime;
    private int accountId;
    private int instrumentId;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private TradeStatus tradeStatus;

    public AccountTrade() {
    }

    public AccountTrade(int accountId, OffsetDateTime tradeTime, int instrumentId, TradeType tradeType, BigDecimal quantity, BigDecimal price) {
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TradeStatus getStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(TradeStatus tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    public OffsetDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(OffsetDateTime tradeTime) {
        this.tradeTime = tradeTime;
    }
}
