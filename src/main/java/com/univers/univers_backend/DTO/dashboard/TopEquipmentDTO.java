/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Data Transfer Object for a top piece of equipment, including its name and"
                        + " reservation counts by status.")
public record TopEquipmentDTO(
        @Schema(description = "Name of the equipment", example = "Projector XL2000")
                String equipmentName,
        @Schema(
                        description =
                                "Total number of reservations for this equipment within the queried"
                                        + " period",
                        example = "75")
                long totalReservationCount,
        @Schema(description = "Number of PENDING reservations for this equipment", example = "10")
                Long pendingCount,
        @Schema(description = "Number of APPROVED reservations for this equipment", example = "30")
                Long approvedCount,
        @Schema(description = "Number of REJECTED reservations for this equipment", example = "5")
                Long rejectedCount,
        @Schema(description = "Number of CANCELED reservations for this equipment", example = "5")
                Long canceledCount,
        @Schema(
                        description =
                                "Number of ONGOING (picked up) reservations for this equipment",
                        example = "15")
                Long ongoingCount,
        @Schema(
                        description =
                                "Number of COMPLETED (returned) reservations for this equipment",
                        example = "10")
                Long completedCount) {}
