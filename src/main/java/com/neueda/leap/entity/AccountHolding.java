package com.neueda.leap.entity;

import java.math.BigDecimal;

import java.time.LocalDateTime;

public class AccountHolding {

    public enum HoldingStatus {
        ACTIVE, INACTIVE
    }

    private int accountId;
    private int instrumentId;
    private LocalDateTime asOfDate;
    private BigDecimal quantity;
    private HoldingStatus status;

    public AccountHolding() {
    }

    public AccountHolding(int accountId, int instrumentId, LocalDateTime asOfDate, BigDecimal quantity, HoldingStatus status) {
        this.accountId = accountId;
        this.instrumentId = instrumentId;
        this.asOfDate = asOfDate;
        this.quantity = quantity;
        this.status = status;
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

    public LocalDateTime getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDateTime asOfDate) {
        this.asOfDate = asOfDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public HoldingStatus getStatus() {
        return status;
    }

    public void setStatus(HoldingStatus status) {
        this.status = status;
    }
}
