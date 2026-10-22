package com.neueda.leap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

// the client details collected at registration, used to open the user's first trading account
public record OpenAccountRequestDTO(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotNull @Past LocalDate birthDate
) {}
