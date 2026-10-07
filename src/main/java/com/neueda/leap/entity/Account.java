package com.neueda.leap.entity;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.UUID;

public class Account {

    private Long accountId;
    private UUID externalAccountId;
    private String accountType;
    private Long clientId;
    private BigDecimal balance;
    HashMap<Instrument, AccountHolding> holdings;

    public Account() {
        this.accountId = null;
        this.holdings = new HashMap<>();
        this.balance = BigDecimal.ZERO;
    }

    public Account(String accountType, Long clientId) {
        this.accountId = null;
        this.externalAccountId = UUID.randomUUID();
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

    public UUID getExternalAccountId() {
        return externalAccountId;
    }

    public void setExternalAccountId(UUID externalAccountId) {
        this.externalAccountId = externalAccountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
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
