package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDTO(
        @NotBlank @Email String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        Long departmentId,
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        Boolean emailVerified) {
}
