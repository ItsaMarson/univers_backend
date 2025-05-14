/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
        description =
                "Data Transfer Object for recent activity items, showing event details and user"
                        + " actions.")
public record RecentActivityItemDTO(
        @Schema(
                        description = "Public ID of the entity",
                        example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
                String id,
        @Schema(description = "User-friendly type of the entity", example = "Event") String type,
        @Schema(description = "Title of the entity", example = "Event Name") String title,
        @Schema(
                        description = "Description of the activity",
                        example = "Status updated to 'APPROVED'")
                String description,
        @Schema(description = "Timestamp of the activity", example = "2023-05-15T10:30:00Z")
                Instant timestamp,
        @Schema(description = "Name of the user who initiated the activity", example = "John Doe")
                String actorName,
        @Schema(
                        description =
                                "Optional: A relative path for frontend linking e.g."
                                        + " '/app/events/details/123'",
                        example = "/app/events/details/123")
                String entityPath) {}
