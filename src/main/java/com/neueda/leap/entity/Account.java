package com.neueda.leap.entity;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private Long accountId;
    private String accountType;
    private final int clientId;
    private double balance;
    HashMap<Instrument, AccountHolding> holdings;

    public Account(String accountType, int clientId) {
        this.accountId = null;
        this.accountType = accountType;
        this.clientId = clientId;
        this.holdings = new HashMap<>();
        balance = 0;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public int getClientId() {
        return clientId;
    }

    public HashMap<Instrument, AccountHolding> getHoldings() {
        return holdings;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

}
