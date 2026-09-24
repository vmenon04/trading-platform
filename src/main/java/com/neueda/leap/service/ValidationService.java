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
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
