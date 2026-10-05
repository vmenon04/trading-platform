package com.neueda.leap.entity;

import com.neueda.leap.enums.Status;

import java.time.LocalDate;

public class AccountSubscription {

    private int accountId;
    private int modelPortfolioId;
    private LocalDate subscriptionDate;
    private Status status;

    public AccountSubscription() {
    }

    public AccountSubscription(int accountId, int modelPortfolioId, LocalDate subscriptionDate) {
        this.accountId = accountId;
        this.modelPortfolioId = modelPortfolioId;
        this.subscriptionDate = subscriptionDate;
        this.status = Status.ACTIVE;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public void setModelPortfolioId(int modelPortfolioId) {
        this.modelPortfolioId = modelPortfolioId;
    }

    public LocalDate getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(LocalDate subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
