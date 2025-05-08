/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.CreateEventRequestDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UpdateEventRequestDTO;
import com.univers.univers_backend.Service.EventService;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(
            @RequestPart("event") CreateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {

        try {
            EventDTO createdEvent =
                    eventService.createEvent(requestDTO, approvedLetterFile, eventImageFile);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while creating the event.");
        }
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<EventDTO> allEvents = eventService.getAllEvents();
        return ResponseEntity.ok(allEvents);
    }

    @GetMapping("/approved")
    public ResponseEntity<List<EventDTO>> getApprovedEvents() {
        List<EventDTO> approvedEvents = eventService.getApprovedEvents();
        return ResponseEntity.ok(approvedEvents);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable UUID eventId) {
        try {
            EventDTO event = eventService.getEventByPublicId(eventId);
            return ResponseEntity.ok(event);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error getting event by ID " + eventId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable UUID eventId,
            @RequestPart("event") UpdateEventRequestDTO requestDTO,
            @RequestPart(value = "approvedLetter", required = false)
                    MultipartFile approvedLetterFile,
            @RequestPart(value = "eventImage", required = false) MultipartFile eventImageFile) {
        try {
            EventDTO updatedEvent =
                    eventService.updateEvent(
                            eventId, requestDTO, approvedLetterFile, eventImageFile);
            return ResponseEntity.ok(updatedEvent);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error updating event " + eventId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while updating the event.");
        }
    }

    @PatchMapping("/{eventId}/cancel")
    public ResponseEntity<String> cancelEvent(
            @PathVariable UUID eventId,
            @RequestParam(required = false, defaultValue = "No reason provided.") String reason) {
        try {
            String responseMessage = eventService.cancelEvent(eventId, reason);
            return ResponseEntity.ok(responseMessage);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error cancelling event " + eventId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable UUID eventId) {
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok("Event with ID " + eventId + " deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error deleting event " + eventId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while deleting the event.");
        }
    }

    @GetMapping("/pending/venue-owner")
    @PreAuthorize("hasAuthority('VENUE_OWNER')")
    public ResponseEntity<List<EventDTO>> getPendingVenueOwnerEvents() {
        try {
            List<EventDTO> pendingEvents = eventService.getPendingEventsForVenueOwner();
            return ResponseEntity.ok(pendingEvents);
        } catch (Exception e) {
            System.err.println("Error getting pending venue owner events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/pending/department-head")
    @PreAuthorize("hasAuthority('DEPT_HEAD')")
    public ResponseEntity<List<EventDTO>> getPendingDeptHeadEvents() {
        try {
            List<EventDTO> pendingEvents = eventService.getPendingEventsForDeptHead();
            return ResponseEntity.ok(pendingEvents);
        } catch (Exception e) {
            System.err.println("Error getting pending department head events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}
