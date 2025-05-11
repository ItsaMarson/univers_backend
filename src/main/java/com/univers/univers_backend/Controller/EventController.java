/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.CreateEventRequestDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UpdateEventRequestDTO;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "Get all events", description = "Retrieves a list of all events")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventDTO>>> getAllEvents() {
        List<EventDTO> allEvents = eventService.getAllEvents();
        return ResponseEntity.ok(ApiResponse.success(allEvents));
    }

    @Operation(
            summary = "Get approved events",
            description = "Retrieves a list of all approved events")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Approved events retrieved successfully")
            })
    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getApprovedEvents() {
        List<EventDTO> approvedEvents = eventService.getApprovedEvents();
        return ResponseEntity.ok(ApiResponse.success(approvedEvents));
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
            summary = "Get pending venue owner events",
            description = "Retrieves pending events for venue owners")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/pending/venue-owner")
    @PreAuthorize("hasAuthority('VENUE_OWNER')")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getPendingVenueOwnerEvents() {
        try {
            List<EventDTO> pendingEvents = eventService.getPendingEventsForVenueOwner();
            return ResponseEntity.ok(ApiResponse.success(pendingEvents));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Error retrieving pending events"));
        }
    }

    @Operation(
            summary = "Get pending department head events",
            description = "Retrieves pending events for department heads")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/pending/department-head")
    @PreAuthorize("hasAuthority('DEPT_HEAD')")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getPendingDeptHeadEvents() {
        try {
            List<EventDTO> pendingEvents = eventService.getPendingEventsForDeptHead();
            return ResponseEntity.ok(ApiResponse.success(pendingEvents));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Error retrieving pending events"));
        }
    }
}
