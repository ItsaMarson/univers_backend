/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.UUID;

public record EventApprovalDTO(
        UUID publicId,
        UUID eventPublicId,
        UserDTO signedByUser,
        String userRole,
        String remarks,
        String status,
        Instant dateSigned) {}
