/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Service.EventService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Event management APIs")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(
            summary = "Create a new event",
            description = "Creates a new event with optional files")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Event created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping
    public ResponseEntity<ApiResponse<EventDTO>> createEvent(
            @RequestPart("event") CreateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {

        try {
            EventDTO createdEvent =
                    eventService.createEvent(requestDTO, approvedLetterFile, eventImageFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Event created successfully", createdEvent));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid input",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating the event"));
        }
    }

    @Operation(
            summary = "Get approved events by venue",
            description = "Retrieves approved events for a specific venue")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/approved/by-venue/{venuePublicId}")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getApprovedEventsByVenue(
            @PathVariable UUID venuePublicId) {
        try {
            List<EventDTO> events = eventService.getApprovedEventsByVenue(venuePublicId);
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Error retrieving events for venue"));
        }
    }

    @Operation(
            summary = "Get ongoing and approved events by venue",
            description = "Retrieves ongoing and approved events for a specific venue")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/ongoing-and-approved/by-venue/{venuePublicId}")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getOngoingAndApprovedEventsByVenue(
            @PathVariable UUID venuePublicId) {
        try {
            List<EventDTO> events = eventService.getOngoingAndApprovedEventsByVenue(venuePublicId);
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Error retrieving ongoing events for venue"));
        }
    }

    @Operation(summary = "Get event by ID", description = "Retrieves a specific event by its ID")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDTO>> getEventById(@PathVariable UUID eventId) {
        try {
            EventDTO event = eventService.getEventByPublicId(eventId);
            return ResponseEntity.ok(ApiResponse.success(event));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred"));
        }
    }

    @Operation(
            summary = "Update event",
            description = "Updates an existing event with optional files")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDTO>> updateEvent(
            @PathVariable UUID eventId,
            @RequestPart("event") UpdateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {
        try {
            EventDTO updatedEvent =
                    eventService.updateEvent(
                            eventId, requestDTO, approvedLetterFile, eventImageFile);
            return ResponseEntity.ok(
                    ApiResponse.success("Event updated successfully", updatedEvent));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid input",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while updating the event"));
        }
    }

    @Operation(summary = "Cancel event", description = "Cancels an existing event")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event cancelled successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{eventId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelEvent(
            @PathVariable UUID eventId,
            @RequestParam(required = false, defaultValue = "No reason provided.") String reason) {
        try {
            String responseMessage = eventService.cancelEvent(eventId, reason);
            return ResponseEntity.ok(ApiResponse.success(responseMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Delete event", description = "Deletes an existing event")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Operation not allowed"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(@PathVariable UUID eventId) {
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok(ApiResponse.success("Event deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.FORBIDDEN.value(),
                                    "Operation not allowed",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while deleting the event"));
        }
    }

    @Operation(
            summary = "Search and filter events",
            description = "Retrieves a list of events based on scope and optional status filter.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid scope or status parameter"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EventDTO>>> searchEvents(
            @RequestParam String scope,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "default") String sortBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<EventDTO> events =
                    eventService.searchEvents(scope, status, sortBy, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid parameter",
                                    e.getMessage()));
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while searching events"));
        }
    }

    @Operation(
            summary = "Get timeline events by date range",
            description =
                    "Retrieves events suitable for a timeline view, filtered by an optional date"
                            + " range. Filters out PENDING, CANCELED, REJECTED events.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Timeline events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid date parameters"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getTimelineEventsByDateRange(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<EventDTO> events = eventService.getTimelineEventsByDateRange(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid date parameters",
                                    e.getMessage()));
        } catch (Exception e) {
            // Log the exception details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while fetching timeline events"));
        }
    }

    @Operation(summary = "Add new personnel", description = "Adds a new assigned personnel")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Personnel added successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/{eventId}/personnel")
    public ResponseEntity<ApiResponse<List<EventPersonnelDTO>>> addPersonnel(
            @PathVariable UUID eventId, @RequestBody EventPersonnelDTO requestDTO) {

        try {
            List<EventPersonnelDTO> listOfPersonnel =
                    eventService.addPersonnel(eventId, requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                            ApiResponse.success(
                                    "Successfully assigned person-in-charge", listOfPersonnel));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating the event"));
        }
    }

    @Operation(summary = "Delete personnel", description = "delete assigned personnel")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Personnel deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("{eventId}/personnel/{publicId}")
    public ResponseEntity<ApiResponse<String>> deletePersonnel(
            @PathVariable UUID eventId, @PathVariable UUID publicId) {
        try {
            if (eventId == null || publicId == null) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        "Event Id or Personnel Id cannot be null"));
            }
            eventService.deletePersonnel(eventId, publicId);
            return ResponseEntity.ok(ApiResponse.success("Personnel(s) deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid UUID format",
                                    e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.FORBIDDEN.value(), "Access denied", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while deleting",
                                    e.getMessage()));
        }
    }
}
