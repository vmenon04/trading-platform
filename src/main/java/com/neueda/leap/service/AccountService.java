package com.neueda.leap.service;

import com.neueda.leap.repository.AccountMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public BigDecimal getBalance(int accountId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void deposit(int accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void withdraw(int accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
