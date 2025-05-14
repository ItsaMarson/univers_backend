/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for event counts over a period, typically by date.")
public record EventCountDTO(
        @Schema(
                        description = "The date for which the event count is reported",
                        example = "2023-05-15")
                LocalDate date,
        @Schema(description = "Total number of events on this date", example = "15")
                long eventCount) {}
