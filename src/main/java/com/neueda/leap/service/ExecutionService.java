package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes validated orders by applying coordinated cash and holding updates.
 */
@Service
public class ExecutionService {

    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;

    /**
     * Creates an execution service for coordinated balance and holding updates.
     *
     * @param accountService service used to adjust account cash balances
     * @param accountHoldingService service used to adjust account holdings
     */
    public ExecutionService(AccountService accountService, AccountHoldingService accountHoldingService) {
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
    }

    /**
     * Executes a validated order as a single transaction.
     *
     * @param order order request to execute
     * @param price execution price used to calculate the total consideration
     * @throws IllegalArgumentException if the order side is unsupported
     * @throws RuntimeException if underlying balance or holding updates fail
     */
    @Transactional
    public void execute(OrderRequest order, BigDecimal price) {
        int accountId = order.accountId();
        int instrumentId = order.instrumentId();
        BigDecimal quantity = order.quantity();
        BigDecimal total = price.multiply(quantity);

        if ("BUY".equals(order.side())) {
            accountService.purchase(accountId, total);
            accountHoldingService.addQuantity(accountId, instrumentId, quantity);
        } else if ("SELL".equals(order.side())) {
            accountHoldingService.removeQuantity(accountId, instrumentId, quantity);
            accountService.sell(accountId, total);
        } else {
            throw new IllegalArgumentException("Side must be BUY or SELL, got " + order.side());
        }
    }
}
