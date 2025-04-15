package com.univers.univers_backend.DTO;


import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String eventName,
        String eventType,
        Long organizerId,

        Long eventVenueId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
