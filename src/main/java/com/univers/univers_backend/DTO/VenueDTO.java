/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record VenueDTO(
        UUID publicId,
        String name,
        String location,
        // Long venueOwnerId,
        UserDTO venueOwner,
        String imagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
