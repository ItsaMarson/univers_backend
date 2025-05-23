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
        @Schema(description = "Number of events with APPROVED status on this date", example = "5")
                long approvedCount,
        @Schema(description = "Number of events with PENDING status on this date", example = "3")
                long pendingCount,
        @Schema(description = "Number of events with CANCELED status on this date", example = "1")
                long canceledCount,
        @Schema(description = "Number of events with REJECTED status on this date", example = "1")
                long rejectedCount,
        @Schema(description = "Number of events with ONGOING status on this date", example = "2")
                long ongoingCount,
        @Schema(description = "Number of events with COMPLETED status on this date", example = "3")
                long completedCount) {}
