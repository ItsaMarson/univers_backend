/* (C)2025 */
package com.univers.univers_backend.DTO;

// import java.time.LocalDateTime; // Removed as createdAt/updatedAt are removed
import java.util.UUID; // Added import

public record EditUserDTO(
        String email,
        String firstName,
        String lastName,
        String password, // Consider if password changes should be via a separate endpoint/DTO
        String idNumber,
        String phoneNumber,
        String telephoneNumber,
        String role,
        // Long departmentId, // Removed departmentId
        UUID departmentPublicId, // Added departmentPublicId
        Boolean emailVerified, // Usually server-controlled or via specific verification flow
        Boolean active,
        String profileImagePath
        // LocalDateTime createdAt, // Removed, server-set
        // LocalDateTime updatedAt // Removed, server-set
        ) {}
