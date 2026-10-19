package com.neueda.leap.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeResponseDTO(
        Long instrumentId,
        String ticker,
        UUID tradeId,
        String side,
        BigDecimal quantity,
        BigDecimal ppu,
        BigDecimal totalPrice,
        String status,
        String reason
) {}
