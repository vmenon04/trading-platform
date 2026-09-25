package com.neueda.leap.service;

import com.neueda.leap.repository.AccountHoldingMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Manages active account holding quantities by creating new holding snapshots for position changes.
 */
@Service
public class AccountHoldingService {

    // Must match the CHECK constraint on account_holdings.status (I think we should change it to capitalization)
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

    private final AccountHoldingMapper accountHoldingMapper;

    /**
     * Creates an account holding service backed by the holding mapper.
     *
     * @param accountHoldingMapper mapper used to query and store holding snapshots
     */
    public AccountHoldingService(AccountHoldingMapper accountHoldingMapper) {
        this.accountHoldingMapper = accountHoldingMapper;
    }

    /**
     * Returns the active quantity held for an instrument in an account.
     *
     * @param accountId account identifier
     * @param instrumentId instrument identifier
     * @return active quantity, or {@link BigDecimal#ZERO} when no active holding exists
     */
    public BigDecimal getQuantity(int accountId, int instrumentId) {
        BigDecimal quantity = accountHoldingMapper.findActiveQuantity(accountId, instrumentId);
        return quantity == null ? BigDecimal.ZERO : quantity;
    }

    /**
     * Increases the active quantity held for an instrument in an account.
     *
     * @param accountId account identifier
     * @param instrumentId instrument identifier
     * @param quantity positive quantity to add
     * @throws IllegalArgumentException if the quantity is null or not positive
     */
    public void addQuantity(int accountId, int instrumentId, BigDecimal quantity) {
        requirePositive(quantity);
        BigDecimal newQuantity = getQuantity(accountId, instrumentId).add(quantity);
        accountHoldingMapper.deactivateHolding(accountId, instrumentId);
        accountHoldingMapper.insertSnapshot(accountId, instrumentId, newQuantity, ACTIVE);
    }

    /**
     * Decreases the active quantity held for an instrument in an account.
     *
     * @param accountId account identifier
     * @param instrumentId instrument identifier
     * @param quantity positive quantity to remove
     * @throws IllegalArgumentException if the quantity is null or not positive
     * @throws IllegalStateException if the account holds less than the requested quantity
     */
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
