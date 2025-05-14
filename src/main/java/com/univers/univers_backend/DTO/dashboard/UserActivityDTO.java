/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Data Transfer Object for user activity, showing event counts per user.")
public record UserActivityDTO(
        @Schema(
                        description = "Public ID of the user",
                        example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
                UUID userPublicId,
        @Schema(description = "First name of the user", example = "John") String userFirstName,
        @Schema(description = "Last name of the user", example = "Doe") String userLastName,
        @Schema(
                        description =
                                "Total number of events organized by this user within the queried"
                                        + " period",
                        example = "15")
                long eventCount) {}
