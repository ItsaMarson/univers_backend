/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Data Transfer Object for a top venue, including its name and event counts by"
                        + " status.")
public record TopVenueDTO(
        @Schema(description = "Name of the venue", example = "Conference Room Alpha")
                String venueName,
        @Schema(
                        description =
                                "Total number of events held at this venue within the queried"
                                        + " period",
                        example = "120")
                long totalEventCount,
        @Schema(description = "Number of APPROVED events at this venue", example = "60")
                Long approvedCount,
        @Schema(description = "Number of PENDING events at this venue", example = "10")
                Long pendingCount,
        @Schema(description = "Number of CANCELED events at this venue", example = "5")
                Long canceledCount,
        @Schema(description = "Number of REJECTED events at this venue", example = "5")
                Long rejectedCount,
        @Schema(description = "Number of ONGOING events at this venue", example = "20")
                Long ongoingCount,
        @Schema(description = "Number of COMPLETED events at this venue", example = "20")
                Long completedCount) {}
