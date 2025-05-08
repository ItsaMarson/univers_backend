/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VenueReservationDTO(
        UUID publicId,
        EventDTO event,
        UserDTO requestingUser,
        DepartmentDTO department,
        VenueDTO venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        List<VenueApprovalDTO> approvals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
