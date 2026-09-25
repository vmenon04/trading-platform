package com.neueda.leap.entity;

import java.time.LocalDate;

public class AccountSubscription {

    public enum SubscriptionStatus {
        ACTIVE, INACTIVE
    }
    private final int accountId;
    private final int modelPortfolioId;
    private final LocalDate subscriptionDate;
    private SubscriptionStatus status;

    public AccountSubscription(int accountId, int modelPortfolioId, LocalDate subscriptionDate) {
        this.accountId = accountId;
        this.modelPortfolioId = modelPortfolioId;
        this.subscriptionDate = subscriptionDate;
        this.status = SubscriptionStatus.ACTIVE;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public LocalDate getSubscriptionDate() {
        return subscriptionDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }
}
