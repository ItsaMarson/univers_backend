/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        Long eventId,
        Object message,
        LocalDateTime createdAt,
        boolean isRead,
        Long relatedEntityId,
        String relatedEntityType) {}
