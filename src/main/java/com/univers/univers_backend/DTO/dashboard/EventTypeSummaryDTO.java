/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data Transfer Object for event type summary, showing event counts per type.")
public record EventTypeSummaryDTO(
        @Schema(description = "Name of the event type", example = "Conference") String name,
        @Schema(description = "Count of events for this type", example = "15") Long value) {}
