package com.neueda.leap.service;

import com.neueda.leap.dto.OrderRequest;
import com.neueda.leap.repository.AccountTradeMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderManagementService {

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

    public synchronized int placeOrder(OrderRequest order) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
