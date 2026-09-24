package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import com.neueda.leap.repository.AccountTradeMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OrderManagementService {

    // must match the CHECK constraint on account_trades.status ( i say make this caps later )
    private static final String PENDING = "pending";
    private static final String ACCEPTED = "accepted";
    private static final String REJECTED = "rejected";
    private static final String FULFILLED = "fulfilled";

    private final InstrumentService instrumentService;
    private final ValidationService validationService;
    private final ExecutionService executionService;
    private final AccountTradeMapper accountTradeMapper;

    public OrderManagementService(InstrumentService instrumentService, ValidationService validationService,
                                  ExecutionService executionService, AccountTradeMapper accountTradeMapper) {
        this.instrumentService = instrumentService;
        this.validationService = validationService;
        this.executionService = executionService;
        this.accountTradeMapper = accountTradeMapper;
    }

    // sequential order execution (prevents concurrent modifications)
    public synchronized int placeOrder(OrderRequest order) {
        BigDecimal price = instrumentService.getCurrentPrice(order.instrumentId());

        try {
            validationService.validate(order, price);
        } catch (IllegalStateException e) {
            recordTrade(order, price, REJECTED);
            throw e;
        }

        int tradeId = recordTrade(order, price, PENDING);
        accountTradeMapper.updateStatus(tradeId, ACCEPTED);

        try {
            executionService.execute(order, price);
        } catch (RuntimeException e) {
            accountTradeMapper.updateStatus(tradeId, REJECTED);
            throw e;
        }

        accountTradeMapper.updateStatus(tradeId, FULFILLED);
        return tradeId;
    }

    private int recordTrade(OrderRequest order, BigDecimal price, String status) {
        return accountTradeMapper.insertTrade(order.accountId(), order.instrumentId(), order.side(),
                order.quantity(), price, status);
    }
}
