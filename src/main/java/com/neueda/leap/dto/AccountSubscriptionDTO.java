package com.neueda.leap.dto;

public class AccountSubscriptionDTO {
    private int accountId;
    private int modelPortfolioId;
    private String subscriptionDate;
    private String status;

    public int getAccountId() {
        return accountId;
    }

    public int getModelPortfolioId() {
        return modelPortfolioId;
    }

    public String getSubscriptionDate() {
        return subscriptionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
