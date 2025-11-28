/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityLogDTO(
        UUID publicId,
        String action,
        String entityType,
        UUID entityId,
        String userEmail,
        String details,
        String ipAddress,
        LocalDateTime createdAt) {}
