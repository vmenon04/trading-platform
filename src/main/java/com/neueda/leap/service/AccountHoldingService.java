package com.neueda.leap.service;

import com.neueda.leap.repository.AccountHoldingMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class AccountHoldingService {

    private final AccountHoldingMapper accountHoldingMapper;

    public AccountHoldingService(AccountHoldingMapper accountHoldingMapper) {
        this.accountHoldingMapper = accountHoldingMapper;
    }

    public BigDecimal getQuantity(int accountId, int instrumentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void addQuantity(int accountId, int instrumentId, BigDecimal quantity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void removeQuantity(int accountId, int instrumentId, BigDecimal quantity) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
