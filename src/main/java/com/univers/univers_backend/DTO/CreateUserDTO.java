/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateUserDTO(
        @NotBlank @Email String email,
        @NotBlank String password,
        String firstName,
        String lastName,
        UUID departmentPublicId,
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        Role role,
        Boolean emailVerified) {}
