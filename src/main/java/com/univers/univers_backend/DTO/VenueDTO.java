package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record VenueDTO(
        String name,
        String location,
        Long venueOwnerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
