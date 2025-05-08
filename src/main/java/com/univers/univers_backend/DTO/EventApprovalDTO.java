/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventApprovalDTO(
        UUID publicId,
        UUID eventPublicId,
        UserDTO signedByUser,
        String userRole,
        String remarks,
        String status,
        LocalDateTime dateSigned) {}
