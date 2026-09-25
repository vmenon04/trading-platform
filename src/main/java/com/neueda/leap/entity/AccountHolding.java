package com.neueda.leap.entity;

import java.time.LocalDate;

public class AccountHolding {

    public enum HoldingStatus {
        ACTIVE, INACTIVE
    }

    private int accountId;
    private int instrumentId;
    private final LocalDate asOfDate;
    private double quantity;
    private HoldingStatus status;

    public AccountHolding(int accountId, int instrumentId, LocalDate asOfDate, double quantity, HoldingStatus status) {
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

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public HoldingStatus getStatus() {
        return status;
    }

    public void setStatus(HoldingStatus status) {
        this.status = status;
    }
}
