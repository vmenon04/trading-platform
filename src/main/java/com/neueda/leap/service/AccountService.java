package com.neueda.leap.service;

import com.neueda.leap.repository.AccountMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    public BigDecimal getBalance(int accountId) {
        BigDecimal balance = accountMapper.findBalance(accountId);
        if (balance == null) {
            throw new NoSuchElementException("No account with id " + accountId);
        }
        return balance;
    }

    public void deposit(int accountId, BigDecimal amount) {
        requirePositive(amount);
        if (accountMapper.increaseBalance(accountId, amount) == 0) {
            throw new NoSuchElementException("No account with id " + accountId);
        }
    }

    public void withdraw(int accountId, BigDecimal amount) {
        requirePositive(amount);
        if (accountMapper.reduceBalance(accountId, amount) == 0) {
            BigDecimal balance = getBalance(accountId);
            throw new IllegalStateException(
                    "Insufficient funds in account " + accountId + ": balance " + balance + ", requested " + amount);
        }
    }

    public void purchase(int accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void sell(int accountId, BigDecimal amount) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
