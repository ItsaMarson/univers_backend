/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID publicId,
        String email,
        String firstName,
        String lastName,
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        String role,
        DepartmentDTO department,
        Boolean emailVerified,
        Boolean active,
        String profileImagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
