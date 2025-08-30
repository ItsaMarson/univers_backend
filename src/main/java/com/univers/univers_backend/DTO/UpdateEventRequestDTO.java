/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status; // Assuming Status is an enum
import jakarta.validation.constraints.FutureOrPresent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateEventRequestDTO(
        String eventName, // Optional
        String eventType, // Optional
        UUID organizerPublicId, // Optional
        UUID venuePublicId, // Optional
        UUID departmentPublicId, // Optional, can be null to unassign or not present to keep
        // existing
        @FutureOrPresent Instant startTime, // Optional
        @FutureOrPresent Instant endTime, // Optional
        Status status, // Optional, using the Enum type
        List<String> assignedPersonnel
) {}
