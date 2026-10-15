package com.neueda.leap.dto;

import java.math.BigDecimal;

public record TradeResponseDTO(
        Long instrumentId,
        String ticker,
        Long tradeId,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        String status,
        String reason
) {}
