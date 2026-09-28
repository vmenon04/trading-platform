package com.neueda.leap.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.stream.Collectors;

public record ErrorResponseDTO(

        @NotBlank List<FieldError> fieldErrors,
        @NotBlank String message
) {

    public String getError() {
        return fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(","));
    }

    public String getMessage() {
        return message;
    }
}
