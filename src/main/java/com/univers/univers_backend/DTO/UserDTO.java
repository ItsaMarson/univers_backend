package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDTO(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
