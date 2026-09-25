package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;

public record ErrorResponseDTO(

        @NotBlank String error,
        @NotBlank String message
) {

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
