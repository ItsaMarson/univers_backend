/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID publicId,
        UUID eventPublicId,
        Object message,
        LocalDateTime createdAt,
        boolean isRead,
        UUID relatedEntityPublicId,
        String relatedEntityType) {}
