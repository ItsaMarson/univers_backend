/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for daily cancellation rates.")
public record CancellationRateDTO(
        @Schema(
                        description = "The date for which the cancellation rate is reported",
                        example = "2023-05-15")
                LocalDate date,
        @Schema(description = "Cancellation rate as a percentage (0.0 to 100.0)", example = "10.5")
                double cancellationRate,
        @Schema(
                        description = "Number of events created on this date that were canceled",
                        example = "2")
                long canceledCount,
        @Schema(description = "Total number of events created on this date", example = "19")
                long totalCreatedCount) {}
