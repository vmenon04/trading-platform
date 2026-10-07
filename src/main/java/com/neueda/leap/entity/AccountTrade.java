package com.neueda.leap.entity;

import com.neueda.leap.enums.TradeSide;
import com.neueda.leap.enums.TradeStatus;

import java.math.BigDecimal;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AccountTrade {

    private Long tradeId;
    private UUID externalTradeId;
    private OffsetDateTime tradeTime;
    private Long accountId;
    private Long instrumentId;
    private TradeSide tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private TradeStatus tradeStatus;

    public AccountTrade() {
    }

    public AccountTrade(Long accountId, OffsetDateTime tradeTime, Long instrumentId, TradeSide tradeType, BigDecimal quantity, BigDecimal price) {
        this.tradeId = null;
        this.accountId = accountId;
        this.tradeTime = tradeTime;
        this.instrumentId = instrumentId;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradeStatus = TradeStatus.SUBMITTED;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public UUID getExternalTradeId() {
        return externalTradeId;
    }

    public void setExternalTradeId(UUID externalTradeId) {
        this.externalTradeId = externalTradeId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public TradeSide getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeSide tradeType) {
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
