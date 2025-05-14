/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Data Transfer Object for a top piece of equipment, including its name and"
                        + " reservation count.")
public record TopEquipmentDTO(
        @Schema(description = "Name of the equipment", example = "Projector XL2000")
                String equipmentName,
        @Schema(
                        description =
                                "Total number of reservations for this equipment within the queried"
                                        + " period",
                        example = "75")
                long reservationCount) {}
