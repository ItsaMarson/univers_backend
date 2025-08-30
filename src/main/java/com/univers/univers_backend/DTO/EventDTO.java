/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDTO(
        UUID publicId,
        String eventName,
        String eventType,
        UserDTO organizer,
        VenueDTO eventVenue,
        DepartmentDTO department,
        Instant startTime,
        Instant endTime,
        String status,
        String approvedLetterUrl,
        String imageUrl,
        List<EventApprovalDTO> approvals,
        Instant createdAt,
        Instant updatedAt,
        List<String> assignedPersonnel) {}
