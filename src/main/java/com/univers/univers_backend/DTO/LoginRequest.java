/* (C)2025 */
package com.univers.univers_backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request data")
public record LoginRequest(
        @Schema(description = "User's email address", example = "user@example.com", required = true)
                @NotBlank
                @Email
                String email,
        @Schema(description = "User's password", example = "password123", required = true) @NotBlank
                String password) {}
