package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {

    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;

    public ExecutionService(AccountService accountService, AccountHoldingService accountHoldingService) {
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
    }

    // do transactional so everything gets executed together so if one part fails, the whole operation is rolled back
    @Transactional
    public void execute(OrderRequest order, BigDecimal price) {
        int accountId = order.accountId();
        int instrumentId = order.instrumentId();
        BigDecimal quantity = order.quantity();
        BigDecimal total = price.multiply(quantity);

        if ("BUY".equals(order.side())) {
            accountService.withdraw(accountId, total);
            accountHoldingService.addQuantity(accountId, instrumentId, quantity);
        } else if ("SELL".equals(order.side())) {
            accountHoldingService.removeQuantity(accountId, instrumentId, quantity);
            accountService.deposit(accountId, total);
        } else {
            throw new IllegalArgumentException("Side must be BUY or SELL, got " + order.side());
        }
    }
}
