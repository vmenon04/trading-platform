package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDTO(
        @NotBlank int status,
        @NotBlank String error,
        @NotBlank String message,
        List<FieldError> fieldErrors
) {


// One shape, every error, everywhere in this service. A client never has
// to guess whether an error comes back as a plain string, a nested object,
// or Spring's own default body - it's always this.

    public record FieldError(String field, String message) {}

    public static ErrorResponseDTO of(int status, String error, String message) {
        return new ErrorResponseDTO(status, error, message, List.of());
    }

    public static ErrorResponseDTO withFieldErrors(int status, String error, String message,
                                                List<FieldError> fieldErrors) {
        return new ErrorResponseDTO(status, error, message, fieldErrors);
    }
}
