/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String eventName,
        String eventType,
        UserDTO organizer,
        Long eventVenueId,
        Long departmentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String approvedLetterUrl,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
