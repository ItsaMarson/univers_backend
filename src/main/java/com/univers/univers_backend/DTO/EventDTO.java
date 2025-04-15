package com.univers.univers_backend.DTO;

import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

public record EventDTO(
        Long id,
        String eventName,
        String eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String approvedLetter
) {
}
