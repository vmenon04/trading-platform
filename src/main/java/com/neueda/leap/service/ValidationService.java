package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    private final InstrumentService instrumentService;
    private final AccountService accountService;
    private final AccountHoldingService accountHoldingService;

    public ValidationService(InstrumentService instrumentService, AccountService accountService,
                             AccountHoldingService accountHoldingService) {
        this.instrumentService = instrumentService;
        this.accountService = accountService;
        this.accountHoldingService = accountHoldingService;
    }

    public void validate(OrderRequest order, BigDecimal price) {
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
