package com.neueda.leap.service;

import com.neueda.leap.repository.AccountHoldingMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class AccountHoldingService {

    // Must match the CHECK constraint on account_holdings.status (I think we should change it to capitalization)
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

    private final AccountHoldingMapper accountHoldingMapper;

    public AccountHoldingService(AccountHoldingMapper accountHoldingMapper) {
        this.accountHoldingMapper = accountHoldingMapper;
    }

    public BigDecimal getQuantity(int accountId, int instrumentId) {
        BigDecimal quantity = accountHoldingMapper.findActiveQuantity(accountId, instrumentId);
        return quantity == null ? BigDecimal.ZERO : quantity;
    }

    public void addQuantity(int accountId, int instrumentId, BigDecimal quantity) {
        requirePositive(quantity);
        BigDecimal newQuantity = getQuantity(accountId, instrumentId).add(quantity);
        accountHoldingMapper.deactivateHolding(accountId, instrumentId);
        accountHoldingMapper.insertSnapshot(accountId, instrumentId, newQuantity, ACTIVE);
    }

    public void removeQuantity(int accountId, int instrumentId, BigDecimal quantity) {
        requirePositive(quantity);
        BigDecimal current = getQuantity(accountId, instrumentId);
        if (current.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient quantity of instrument " + instrumentId
                    + " in account " + accountId + ": holding " + current + ", requested " + quantity);
        }
        BigDecimal newQuantity = current.subtract(quantity);
        // a fully sold position should be marked as inactive
        String status = newQuantity.compareTo(BigDecimal.ZERO) == 0 ? INACTIVE : ACTIVE;
        accountHoldingMapper.deactivateHolding(accountId, instrumentId);
        accountHoldingMapper.insertSnapshot(accountId, instrumentId, newQuantity, status);
    }

    private static void requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
