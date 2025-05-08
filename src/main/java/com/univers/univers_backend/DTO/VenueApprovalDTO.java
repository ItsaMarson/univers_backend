/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record VenueApprovalDTO(
        UUID publicId,
        UUID venueReservationPublicId,
        UserDTO signedByUser,
        String userRole,
        String remarks,
        String status,
        LocalDateTime dateSigned) {}
