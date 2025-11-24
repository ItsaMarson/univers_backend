/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Service.EventService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
            @Valid @RequestPart("event") CreateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {

        EventDTO createdEvent =
                eventService.createEvent(requestDTO, approvedLetterFile, eventImageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully", createdEvent));
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
        List<EventDTO> events = eventService.getApprovedEventsByVenue(venuePublicId);
        return ResponseEntity.ok(ApiResponse.success(events));
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
        List<EventDTO> events = eventService.getOngoingAndApprovedEventsByVenue(venuePublicId);
        return ResponseEntity.ok(ApiResponse.success(events));
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
        EventDTO event = eventService.getEventByPublicId(eventId);
        return ResponseEntity.ok(ApiResponse.success(event));
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
            @Valid @RequestPart("event") UpdateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {
        EventDTO updatedEvent =
                eventService.updateEvent(eventId, requestDTO, approvedLetterFile, eventImageFile);
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", updatedEvent));
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
        String responseMessage = eventService.cancelEvent(eventId, reason);
        return ResponseEntity.ok(ApiResponse.success(responseMessage));
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
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully"));
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
        List<EventDTO> events =
                eventService.searchEvents(scope, status, sortBy, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(events));
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
        List<EventDTO> events = eventService.getTimelineEventsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(events));
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
            @PathVariable UUID eventId, @Valid @RequestBody EventPersonnelDTO requestDTO) {

        List<EventPersonnelDTO> listOfPersonnel = eventService.addPersonnel(eventId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Successfully assigned person-in-charge", listOfPersonnel));
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
        eventService.deletePersonnel(eventId, publicId);
        return ResponseEntity.ok(ApiResponse.success("Personnel(s) deleted successfully"));
    }

    @Operation(
            summary = "Get all personnel",
            description = "Retrieves all users with ASSIGNED_PERSONNEL role")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Personnel retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/personnel")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllPersonnel() {
        List<UserDTO> personnel = eventService.getAllPersonnel();
        return ResponseEntity.ok(ApiResponse.success(personnel));
    }
}
