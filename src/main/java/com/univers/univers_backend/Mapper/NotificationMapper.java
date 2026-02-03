/* (C)2025-2026 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.NotificationDTO;
import com.univers.univers_backend.Entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDTO toDto(Notification notification) {
        if (notification == null) {
            return null;
        }

        return new NotificationDTO(
                notification.getPublicId(),
                notification.getEventPublicId(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getRelatedEntityPublicId(),
                notification.getRelatedEntityType());
    }

    // public Notification toEntity(NotificationDTO dto) { ... } // If needed later
}
