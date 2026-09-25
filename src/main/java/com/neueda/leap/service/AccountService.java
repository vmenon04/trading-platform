package com.neueda.leap.service;

import com.neueda.leap.repository.AccountMapper;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Provides account cash balance lookups and balance adjustments for cash movements and trade execution.
 */
@Service
public class AccountService {

    private final AccountMapper accountMapper;

    /**
     * Creates an account service backed by the account mapper.
     *
     * @param accountMapper mapper used to query and update account balances
     */
    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    /**
     * Returns the current balance for an account.
     *
     * @param accountId account identifier
     * @return current account balance
     * @throws NoSuchElementException if the account does not exist
     */
    public BigDecimal getBalance(int accountId) {
        BigDecimal balance = accountMapper.findBalance(accountId);
        if (balance == null) {
            throw new NoSuchElementException("No account with id " + accountId);
        }
        return balance;
    }

    /**
     * Adds client cash to an account.
     *
     * @param accountId account identifier
     * @param amount positive amount to deposit
     * @throws IllegalArgumentException if the amount is null or not positive
     * @throws NoSuchElementException if the account does not exist
     */
    public void deposit(int accountId, BigDecimal amount) {
        increaseBalance(accountId, amount);
    }

    /**
     * Removes client cash from an account.
     *
     * @param accountId account identifier
     * @param amount positive amount to withdraw
     * @throws IllegalArgumentException if the amount is null or not positive
     * @throws IllegalStateException if the account has insufficient funds
     */
    public void withdraw(int accountId, BigDecimal amount) {
        reduceBalance(accountId, amount);
    }

    /**
     * Deducts cash from an account to settle a purchase.
     *
     * @param accountId account identifier
     * @param amount positive cash amount to remove
     * @throws IllegalArgumentException if the amount is null or not positive
     * @throws IllegalStateException if the account has insufficient funds
     */
    public void purchase(int accountId, BigDecimal amount) {
        reduceBalance(accountId, amount);
    }

    /**
     * Credits cash to an account after a sale.
     *
     * @param accountId account identifier
     * @param amount positive cash amount to add
     * @throws IllegalArgumentException if the amount is null or not positive
     * @throws NoSuchElementException if the account does not exist
     */
    public void sell(int accountId, BigDecimal amount) {
        increaseBalance(accountId, amount);
    }

    private void increaseBalance(int accountId, BigDecimal amount) {
        requirePositive(amount);
        if (accountMapper.increaseBalance(accountId, amount) == 0) {
            throw new NoSuchElementException("No account with id " + accountId);
        }
    }

    private void reduceBalance(int accountId, BigDecimal amount) {
        requirePositive(amount);
        if (accountMapper.reduceBalance(accountId, amount) == 0) {
            BigDecimal balance = getBalance(accountId);
            throw new IllegalStateException(
                    "Insufficient funds in account " + accountId + ": balance " + balance + ", requested " + amount);
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
