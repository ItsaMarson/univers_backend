/* (C)2025 */
package com.univers.univers_backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "User registration data")
public record RegisterDTO(
        @Schema(description = "User's email address", example = "user@example.com", required = true)
                @NotBlank
                @Email
                String email,
        @Schema(description = "User's password", example = "password123", required = true) @NotBlank
                String password,
        @Schema(description = "User's first name", example = "John") String firstName,
        @Schema(description = "User's last name", example = "Doe") String lastName,
        @Schema(
                        description = "Department's public ID",
                        example = "123e4567-e89b-12d3-a456-426614174000")
                UUID departmentPublicId,
        @Schema(description = "User's ID number", example = "12345") String idNumber,
        @Schema(description = "User's mobile phone number", example = "+1234567890")
                String phoneNumber,
        @Schema(description = "User's telephone number", example = "+1234567890")
                String telephoneNumber,
        @Schema(description = "Whether the user's email is verified", example = "false")
                Boolean emailVerified) {}
