/* (C)2025 */
package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepartmentDTO(
        UUID publicId,
        @NotBlank String name,
        String description,
        UserDTO deptHead,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
