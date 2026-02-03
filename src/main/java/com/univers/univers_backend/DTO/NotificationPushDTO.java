/* (C)2026 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.UUID;

/** DTO for pushing notifications via SSE. */
public record NotificationPushDTO(
        UUID publicId,
        UUID recipientPublicId,
        NotificationMessageDTO message,
        UUID eventPublicId,
        UUID relatedEntityPublicId,
        String relatedEntityType,
        Instant createdAt,
        boolean isRead) {

    public record NotificationMessageDTO(String message, String type) {
        public NotificationMessageDTO(String message) {
            this(message, "info");
        }
    }
}
