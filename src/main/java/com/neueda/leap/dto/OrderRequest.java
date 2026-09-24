package com.neueda.leap.dto;

import java.math.BigDecimal;

// TODO: temporary, waiting for DTO implementation
public record OrderRequest(int accountId, int instrumentId, String side, BigDecimal quantity) {
}
