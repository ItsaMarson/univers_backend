/* (C)2025 */
package com.univers.univers_backend.DTO;

// Removed import for com.univers.univers_backend.Enum.EventType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateEventRequestDTO(
        @NotBlank String eventName,
        @NotBlank String eventType,
        @NotNull UUID venuePublicId,
        UUID departmentPublicId,
        @NotNull @FutureOrPresent Instant startTime,
        @NotNull @FutureOrPresent Instant endTime) {}
