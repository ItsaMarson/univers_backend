/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
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
        Instant createdAt,
        Instant updatedAt) {}
