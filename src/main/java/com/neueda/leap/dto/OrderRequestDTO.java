package com.neueda.leap.dto;

import com.neueda.leap.enums.Side;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderRequestDTO(
        @Positive UUID accountId,
        @Positive int instrumentId,
        @NotBlank Side side,
        @NotNull @Positive BigDecimal quantity
//        @NotNull @Positive BigDecimal quote
) {


}
