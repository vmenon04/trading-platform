package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import java.math.BigDecimal;
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
    public void validate(OrderRequestDTO order, BigDecimal price) {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }
        String side = order.side();
        if (!"BUY".equals(side) && !"SELL".equals(side)) {
            throw new IllegalArgumentException("Side must be BUY or SELL, got " + side);
        }
        BigDecimal quantity = order.quantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity.stripTrailingZeros().scale() > 8) {
            throw new IllegalArgumentException("Quantity can have at most 8 decimal places, got " + quantity);
        }

        int accountId = order.accountId();
        int instrumentId = order.instrumentId();
        instrumentService.getInstrumentById(instrumentId);
        BigDecimal balance = accountService.getBalance(accountId);

        if ("BUY".equals(side)) {
            BigDecimal cost = price.multiply(quantity);
            if (balance.compareTo(cost) < 0) {
                throw new IllegalStateException("Insufficient funds in account " + accountId
                        + ": balance " + balance + ", order cost " + cost);
            }
        } else {
            BigDecimal held = accountHoldingService.getQuantity(accountId, instrumentId);
            if (held.compareTo(quantity) < 0) {
                throw new IllegalStateException("Insufficient quantity of instrument " + instrumentId
                        + " in account " + accountId + ": holding " + held + ", order quantity " + quantity);
            }
        }
    }
}
