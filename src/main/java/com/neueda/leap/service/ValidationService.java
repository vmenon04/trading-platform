package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import java.math.BigDecimal;
import java.util.UUID;

import com.neueda.leap.enums.TradeSide;
import org.springframework.stereotype.Service;

/**
 * Validates incoming orders against supported order sides, available cash, and held quantities.
 */
@Service
public class ValidationService {

    private final InstrumentService instrumentService;
    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;

    /**
     * Creates a validation service with access to instrument, account, and holding data.
     *
     * @param instrumentService service used to verify referenced instruments
     * @param accountService service used to inspect account balances
     * @param accountHoldingService service used to inspect account holdings
     */
    public ValidationService(InstrumentService instrumentService, AccountService accountService,
                             AccountHoldingService accountHoldingService) {
        this.instrumentService = instrumentService;
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
    }

    /**
     * Validates that an order is well formed and can be executed at the supplied price.
     *
     * @param order order to validate
     * @param price execution price used to calculate required cash for buy orders
     * @throws IllegalArgumentException if the order, side, or quantity is invalid
     * @throws IllegalStateException if the account lacks enough cash or holdings to satisfy the order
     */
    public void validate(TradeRequestDTO order, BigDecimal price) {
        checkOrderNotNull(order);
        TradeSide side = checkSideValid(order);
        BigDecimal quantity = checkQuantityPositive(order);
        checkDecimalsValid(quantity);

        UUID externalAccountId = order.accountId();
        Long accountId = accountService.getAccountIdByExternalAccountId(externalAccountId);

        Long instrumentId = order.instrumentId();
        instrumentService.getInstrumentById(instrumentId);
        BigDecimal balance = accountService.getBalance(accountId);

        if (TradeSide.BUY.equals(side)) {
            checkSufficientFunds(price, quantity, balance, accountId);
        } else {
            checkSufficientInstrumentQuantity(accountId, instrumentId, quantity);
        }
    }

    private void checkSufficientInstrumentQuantity(Long accountId, Long instrumentId, BigDecimal quantity) {
        BigDecimal held = accountHoldingService.getQuantity(accountId, instrumentId);
        if (held.compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient quantity of instrument " + instrumentId
                    + " in account " + accountId + ": holding " + held + ", order quantity " + quantity);
        }
    }

    private void checkSufficientFunds(BigDecimal price, BigDecimal quantity, BigDecimal balance, Long accountId) {
        BigDecimal cost = price.multiply(quantity);
        if (balance.compareTo(cost) < 0) {
            throw new IllegalStateException("Insufficient funds in account " + accountId
                    + ": balance " + balance + ", order cost " + cost);
        }
    }

    private void checkDecimalsValid(BigDecimal quantity) {
        if (quantity.stripTrailingZeros().scale() > 8) {
            throw new IllegalArgumentException("Quantity can have at most 8 decimal places, got " + quantity);
        }
    }

    private BigDecimal checkQuantityPositive(TradeRequestDTO order) {
        BigDecimal quantity = order.quantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return quantity;
    }

    private TradeSide checkSideValid(TradeRequestDTO order) {
        TradeSide side = order.side();
        if (!TradeSide.BUY.equals(side) && !TradeSide.SELL.equals(side)) {
            throw new IllegalArgumentException("Side must be BUY or SELL, got " + side);
        }
        return side;
    }

    private void checkOrderNotNull(TradeRequestDTO order) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
    }
}
