package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record VenueDTO(

        Long id,
        String name,
        String location,
        // Long venueOwnerId,
        UserDTO venueOwner, 
        String imagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
