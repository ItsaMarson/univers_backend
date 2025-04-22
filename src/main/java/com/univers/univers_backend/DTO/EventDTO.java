package com.univers.univers_backend.DTO;


import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String eventName,
        String eventType,
        //Long organizerId,
        UserDTO organizer,
        String approvedLetterPath,
        Long eventVenueId,
        LocalDateTime startTime,
        LocalDateTime endTime,

        String status
) {
}
