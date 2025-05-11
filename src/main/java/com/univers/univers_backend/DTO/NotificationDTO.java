/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.UUID;

public record NotificationDTO(
        UUID publicId,
        UUID eventPublicId,
        Object message,
        Instant createdAt,
        boolean isRead,
        UUID relatedEntityPublicId,
        String relatedEntityType) {}
