package com.neueda.leap.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountHoldingResponseDTO(
        UUID accountId,
        String name,
        String ticker,
        BigDecimal quantity,
        BigDecimal price
) {}
