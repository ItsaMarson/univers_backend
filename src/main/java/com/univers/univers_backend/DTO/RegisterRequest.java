package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        String role,
        Boolean emailVerified) {
}
