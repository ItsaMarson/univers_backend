/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventDTO(
        UUID publicId,
        String eventName,
        String eventType,
        UserDTO organizer,
        VenueDTO eventVenue,
        DepartmentDTO department,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String approvedLetterUrl,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
