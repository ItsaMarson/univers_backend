/* (C)2025 */
package com.univers.univers_backend.DTO;

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
        Long departmentId,
        Boolean emailVerified,
        Boolean active,
        String profileImagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
