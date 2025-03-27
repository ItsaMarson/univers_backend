package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        Role roles,
        Boolean emailVerified) {
}
