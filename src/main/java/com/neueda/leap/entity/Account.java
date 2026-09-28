package com.neueda.leap.entity;

import java.math.BigDecimal;

import java.util.HashMap;

public class Account {

    private Long accountId;
    private String accountType;
    private int clientId;
    private BigDecimal balance;
    HashMap<Instrument, AccountHolding> holdings;

    public Account() {
        this.accountId = null;
        this.holdings = new HashMap<>();
        this.balance = BigDecimal.ZERO;
    }

    public Account(String accountType, int clientId) {
        this.accountId = null;
        this.accountType = accountType;
        this.clientId = clientId;
        this.holdings = new HashMap<>();
        balance = BigDecimal.ZERO;
    }

    public Long getAccountId() {
        return accountId;
    }

    public  void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public HashMap<Instrument, AccountHolding> getHoldings() {
        return holdings;
    }

    public void setHoldings(HashMap<Instrument, AccountHolding> holdings) {
        this.holdings = holdings;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

}
