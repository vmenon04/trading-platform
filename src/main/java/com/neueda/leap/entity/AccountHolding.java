package com.neueda.leap.entity;

import com.neueda.leap.enums.Status;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AccountHolding {

    private int accountId;
    private int instrumentId;
    private OffsetDateTime asOfDate;
    private BigDecimal quantity;
    private Status status;

    public AccountHolding() {
    }

    public AccountHolding(int accountId, int instrumentId, OffsetDateTime asOfDate, BigDecimal quantity, Status status) {
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

    public OffsetDateTime getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(OffsetDateTime asOfDate) {
        this.asOfDate = asOfDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
