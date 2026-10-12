package com.neueda.leap.entity;

import java.time.LocalDate;

public class AccountSubscription {

    public enum SubscriptionStatus {
        ACTIVE, INACTIVE
    }
    private int accountId;
    private int modelPortfolioId;
    private LocalDate subscriptionDate;
    private SubscriptionStatus status;

    public AccountSubscription() {
    }

    public AccountSubscription(int accountId, int modelPortfolioId, LocalDate subscriptionDate) {
        this.accountId = accountId;
        this.modelPortfolioId = modelPortfolioId;
        this.subscriptionDate = subscriptionDate;
        this.status = SubscriptionStatus.ACTIVE;
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

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }
}
