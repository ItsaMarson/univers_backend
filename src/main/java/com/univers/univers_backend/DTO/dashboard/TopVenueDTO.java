/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data Transfer Object for a top venue, including its name and event count.")
public record TopVenueDTO(
        @Schema(description = "Name of the venue", example = "Conference Room Alpha")
                String venueName,
        @Schema(
                        description =
                                "Total number of events held at this venue within the queried"
                                        + " period",
                        example = "120")
                long eventCount) {}
