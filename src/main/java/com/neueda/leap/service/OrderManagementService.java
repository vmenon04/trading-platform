package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequestDTO;
import com.neueda.leap.repository.AccountTradeMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Coordinates order pricing, validation, execution, and trade status recording.
 */
@Service
public class OrderManagementService {

    // must match the CHECK constraint on account_trades.status
    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String REJECTED = "REJECTED";
    private static final String FULFILLED = "FULFILLED";

    private final InstrumentService instrumentService;
    private final ValidationService validationService;
    private final ExecutionService executionService;
    private final AccountTradeMapper accountTradeMapper;

    /**
     * Creates an order management service with its pricing, validation, execution, and persistence collaborators.
     *
     * @param instrumentService service used to obtain current instrument prices
     * @param validationService service used to validate incoming orders
     * @param executionService service used to apply successful order executions
     * @param accountTradeMapper mapper used to persist trade records and status transitions
     */
    public OrderManagementService(InstrumentService instrumentService, ValidationService validationService,
                                  ExecutionService executionService, AccountTradeMapper accountTradeMapper) {
        this.instrumentService = instrumentService;
        this.validationService = validationService;
        this.executionService = executionService;
        this.accountTradeMapper = accountTradeMapper;
    }

    /**
     * Places an order by pricing it, validating it, recording lifecycle statuses, and executing it.
     *
     * @param order order request to place
     * @return generated trade identifier
     * @throws IllegalArgumentException if the order request is invalid
     * @throws IllegalStateException if validation or execution fails due to business constraints
     */
    public synchronized int placeOrder(OrderRequestDTO order) {
        BigDecimal price = instrumentService.getCurrentPrice(order.instrumentId());

        try {
            validationService.validate(order, price);
        } catch (IllegalStateException e) {
            recordTrade(order, price, REJECTED);
            throw e;
        }

        int tradeId = recordTrade(order, price, PENDING);
        accountTradeMapper.insertStatus(tradeId, ACCEPTED);

        try {
            executionService.execute(order, price);
        } catch (RuntimeException e) {
            accountTradeMapper.insertStatus(tradeId, REJECTED);
            throw e;
        }

        accountTradeMapper.insertStatus(tradeId, FULFILLED);
        return tradeId;
    }

    private int recordTrade(OrderRequestDTO order, BigDecimal price, String status) {
        return accountTradeMapper.insertTrade(order.accountId(), order.instrumentId(), order.side(),
                order.quantity(), price, status);
    }
}
