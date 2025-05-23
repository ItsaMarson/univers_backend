/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        description =
                "Data Transfer Object for user reservation activity, showing reservation counts per"
                        + " user by status.")
public record UserReservationActivityDTO(
        @Schema(
                        description = "Public ID of the user",
                        example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
                UUID userPublicId,
        @Schema(description = "First name of the user", example = "John") String userFirstName,
        @Schema(description = "Last name of the user", example = "Doe") String userLastName,
        @Schema(
                        description =
                                "Total number of reservations made by this user within the queried"
                                        + " period",
                        example = "25")
                long totalReservationCount,
        @Schema(description = "Number of PENDING reservations for this user", example = "5")
                long pendingCount,
        @Schema(description = "Number of APPROVED reservations for this user", example = "10")
                long approvedCount,
        @Schema(description = "Number of REJECTED reservations for this user", example = "2")
                long rejectedCount,
        @Schema(description = "Number of CANCELED reservations for this user", example = "3")
                long canceledCount,
        @Schema(
                        description = "Number of ONGOING (picked up) reservations for this user",
                        example = "4")
                long ongoingCount,
        @Schema(
                        description = "Number of COMPLETED (returned) reservations for this user",
                        example = "1")
                long completedCount) {}
