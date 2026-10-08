package com.neueda.leap.service;

import com.neueda.leap.dto.TradeRequestDTO;
import com.neueda.leap.enums.TradeStatus;
import com.neueda.leap.repository.AccountTradeMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Coordinates order pricing, validation, execution, and trade status recording.
 */
@Service
public class OrderManagementService {

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
    public synchronized Long placeOrder(TradeRequestDTO order) {
        BigDecimal price = instrumentService.getCurrentPrice(order.instrumentId());

        try {
            validationService.validate(order, price);
        } catch (IllegalStateException e) {
            recordTrade(order, price, TradeStatus.REJECTED);
            throw e;
        }

        Long tradeId = recordTrade(order, price, TradeStatus.SUBMITTED);
        accountTradeMapper.insertStatus(tradeId, TradeStatus.ACCEPTED);

        try {
            executionService.execute(order, price);
        } catch (RuntimeException e) {
            accountTradeMapper.insertStatus(tradeId, TradeStatus.REJECTED);
            throw e;
        }

        accountTradeMapper.insertStatus(tradeId, TradeStatus.FULFILLED);
        return tradeId;
    }

    private Long recordTrade(TradeRequestDTO order, BigDecimal price, TradeStatus status) {
        //TODO: Remove: needs to be moved to kafka execution
        throw new UnsupportedOperationException();
    }
}
