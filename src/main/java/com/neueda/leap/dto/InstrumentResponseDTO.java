package com.neueda.leap.dto;

import java.math.BigDecimal;

public record InstrumentResponseDTO(
        Long instrumentId,
        String ticker,
        String name,
        BigDecimal price
) {}
