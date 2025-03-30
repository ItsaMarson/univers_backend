package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Entity.User;
import jakarta.validation.constraints.NotBlank;

public record DepartmentDTO(

        @NotBlank String name,
        String description,
        Long deptHead
) {
}
