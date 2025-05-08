/* (C)2025 */
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

        // Assuming Event and RelatedEntity also have publicId if they are to be fetched by these
        // IDs.
        // The Notification entity itself stores eventId (Long) and relatedEntityId (Long).
        // The DTO wants UUIDs. This implies that when Notification entity was designed,
        // eventId and relatedEntityId were Long. If these refer to entities that now have publicId
        // (UUID),
        // then Notification entity should ideally store those publicIds (UUIDs) directly.
        // Or, the mapping here would need to fetch the Event/RelatedEntity by their Long ID,
        // then get their publicId (UUID) for the DTO. This is inefficient.

        // For now, I'll assume Notification entity will be updated or that these Long IDs are
        // treated differently.
        // Let's assume a direct mapping if possible for the structure, or placeholder for now.
        // The DTO has eventPublicId (UUID) and relatedEntityPublicId (UUID).
        // The Entity has eventId (Long) and relatedEntityId (Long).
        // THIS IS A MISMATCH that needs to be resolved in Entity design or by more complex mapping.

        // Simplistic mapping for now, assuming a way to get publicId from the Long Id, or that
        // entity changes.
        // This part is a placeholder and likely incorrect without more context on how Long eventId
        // maps to UUID eventPublicId
        /*
        UUID eventPublicId = null;
        if (notification.getEventId() != null) {
            // TODO: Logic to convert/fetch Event and get its publicId from notification.getEventId()
        }
        UUID relatedEntityPublicId = null;
        if (notification.getRelatedEntityId() != null) {
            // TODO: Logic to convert/fetch RelatedEntity and get its publicId from notification.getRelatedEntityId()
        }
        */

        return new NotificationDTO(
                notification.getPublicId(),
                null, // Placeholder for eventPublicId - Requires logic based on how Event is linked
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.isRead(),
                null, // Placeholder for relatedEntityPublicId - Requires logic
                notification.getRelatedEntityType());
    }

    // public Notification toEntity(NotificationDTO dto) { ... } // If needed later
}
