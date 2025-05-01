/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record VenueReservationDTO(
        Long id,
        Long eventId,
        String eventName,
        UserDTO requestingUser,
        Long departmentId,
        String departmentName,
        Long venueId,
        String venueName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String reservationLetterUrl,
        List<VenueApprovalDTO> approvals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
