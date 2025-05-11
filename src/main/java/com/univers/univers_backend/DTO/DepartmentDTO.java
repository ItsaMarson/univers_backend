/* (C)2025 */
package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public record DepartmentDTO(
        UUID publicId,
        @NotBlank String name,
        String description,
        UserDTO deptHead,
        Instant createdAt,
        Instant updatedAt) {}
