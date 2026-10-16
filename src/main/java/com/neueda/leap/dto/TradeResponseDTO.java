package com.neueda.leap.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeResponseDTO(
        Long instrumentId,
        String ticker,
        UUID tradeId,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        String status,
        String reason
) {}
