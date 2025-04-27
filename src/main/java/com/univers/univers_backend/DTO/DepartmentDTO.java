/* (C)2025 */
package com.univers.univers_backend.DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record DepartmentDTO(
        Long id,
        @NotBlank String name,
        String description,
        Long deptHead,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
