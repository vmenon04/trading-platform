package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InstrumentDTO(
        @Positive int instrumentId,
        @NotBlank String name,
        @NotBlank String ticker
) {

    public int getInstrumentId() {
        return instrumentId;
    }

    public String getName() {
        return name;
    }

    public String getTicker() {
        return ticker;
    }
}
