package com.neueda.leap.dto;

import jakarta.validation.constraints.Positive;

public record OrderSubmittedDTO(
        @Positive int jobId,
        String status
) {

}
