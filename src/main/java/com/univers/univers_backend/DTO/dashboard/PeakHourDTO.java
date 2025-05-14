/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Data Transfer Object for peak reservation hours, showing event counts per hour of"
                        + " the day.")
public record PeakHourDTO(
        @Schema(description = "The hour of the day (0-23)", example = "14") int hourOfDay,
        @Schema(description = "Total number of events starting in this hour", example = "25")
                long eventCount) {}
