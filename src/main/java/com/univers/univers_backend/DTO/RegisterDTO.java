/* (C)2025 */
package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RegisterDTO(
        @NotBlank @Email String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        UUID departmentPublicId,
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        Boolean emailVerified) {}
