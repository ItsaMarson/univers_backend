package com.univers.univers_backend.DTO;


import com.univers.univers_backend.Entity.Department;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        String role,
        Long department_id,
        Boolean emailVerified,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
