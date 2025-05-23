/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Data Transfer Object for event type summary, showing event counts per type and"
                        + " status.")
public record EventTypeSummaryDTO(
        @Schema(description = "Name of the event type", example = "Conference") String name,
        @Schema(description = "Total count of events for this type", example = "15")
                Long totalCount,
        @Schema(description = "Number of APPROVED events for this type", example = "5")
                Long approvedCount,
        @Schema(description = "Number of PENDING events for this type", example = "3")
                Long pendingCount,
        @Schema(description = "Number of CANCELED events for this type", example = "1")
                Long canceledCount,
        @Schema(description = "Number of REJECTED events for this type", example = "1")
                Long rejectedCount,
        @Schema(description = "Number of ONGOING events for this type", example = "2")
                Long ongoingCount,
        @Schema(description = "Number of COMPLETED events for this type", example = "3")
                Long completedCount) {}
