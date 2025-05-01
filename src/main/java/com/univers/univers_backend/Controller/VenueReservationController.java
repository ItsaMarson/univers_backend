/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.VenueApprovalDTO;
import com.univers.univers_backend.DTO.VenueReservationDTO;
import com.univers.univers_backend.Service.VenueReservationService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/venue-reservations")
public class VenueReservationController {

    private final VenueReservationService venueReservationService;

    public VenueReservationController(VenueReservationService venueReservationService) {
        this.venueReservationService = venueReservationService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VenueReservationDTO>> getOwnReservations() {
        try {
            List<VenueReservationDTO> reservations =
                    venueReservationService.getOwnVenueReservations();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("Error getting own venue reservations: " + e.getMessage());
            // Consider more specific error handling if needed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVenueReservation(
            @RequestPart("reservation") VenueReservationDTO reservationDTO,
            @RequestPart(value = "reservationLetter", required = false)
                    MultipartFile reservationLetterFile) {
        try {
            VenueReservationDTO createdReservation =
                    venueReservationService.createVenueReservation(
                            reservationDTO, reservationLetterFile);
            return new ResponseEntity<>(createdReservation, HttpStatus.CREATED);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating venue reservation: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<VenueReservationDTO>> getAllReservations() {
        List<VenueReservationDTO> reservations = venueReservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()") // Check ownership/role in service layer if needed
    public ResponseEntity<?> getReservationById(@PathVariable Long reservationId) {
        try {
            VenueReservationDTO reservation =
                    venueReservationService.getReservationById(reservationId);
            // Add authorization check here if needed (e.g., is user the requester or admin?)
            return ResponseEntity.ok(reservation);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error getting venue reservation by ID "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{reservationId}/approve")
    @PreAuthorize("hasAuthority('VENUE_OWNER')") // Or other relevant roles
    public ResponseEntity<String> approveReservation(
            @PathVariable Long reservationId, @RequestBody Map<String, String> payload) {
        String remarks = payload.getOrDefault("remarks", "");
        try {
            String responseMessage =
                    venueReservationService.approveReservation(reservationId, remarks);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest().body(responseMessage);
            }
            return ResponseEntity.ok(responseMessage);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error approving venue reservation " + reservationId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred during approval.");
        }
    }

    @PatchMapping("/{reservationId}/reject")
    @PreAuthorize("hasAuthority('VENUE_OWNER')") // Or other relevant roles
    public ResponseEntity<String> rejectReservation(
            @PathVariable Long reservationId, @RequestBody Map<String, String> payload) {
        String remarks = payload.get("remarks");
        if (remarks == null || remarks.isBlank()) {
            return ResponseEntity.badRequest().body("Rejection remarks are required.");
        }
        try {
            String responseMessage =
                    venueReservationService.rejectReservation(reservationId, remarks);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest().body(responseMessage);
            }
            return ResponseEntity.ok(responseMessage);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error rejecting venue reservation " + reservationId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred during rejection.");
        }
    }

    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("isAuthenticated()") // Authorization checked in service
    public ResponseEntity<String> cancelReservation(@PathVariable Long reservationId) {
        try {
            String responseMessage = venueReservationService.cancelReservation(reservationId);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest().body(responseMessage);
            }
            return ResponseEntity.ok(responseMessage);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error cancelling venue reservation " + reservationId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @DeleteMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()") // Authorization checked in service
    public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
        try {
            venueReservationService.deleteReservation(reservationId);
            return ResponseEntity.ok(
                    "Venue reservation with ID " + reservationId + " deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error deleting venue reservation " + reservationId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/{reservationId}/approvals")
    @PreAuthorize("isAuthenticated()") // Or specific roles
    public ResponseEntity<?> getApprovalsForReservation(@PathVariable Long reservationId) {
        try {
            List<VenueApprovalDTO> approvals =
                    venueReservationService.getAllApprovalsForReservation(reservationId);
            return ResponseEntity.ok(approvals);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error getting approvals for reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/pending/venue-owner")
    @PreAuthorize("hasAuthority('VENUE_OWNER')")
    public ResponseEntity<List<VenueReservationDTO>> getPendingVenueOwnerReservations() {
        try {
            List<VenueReservationDTO> pendingReservations =
                    venueReservationService.getPendingReservationsForVenueOwner();
            return ResponseEntity.ok(pendingReservations);
        } catch (Exception e) {
            System.err.println("Error getting pending venue owner reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/all/venue-owner")
    @PreAuthorize("hasAuthority('VENUE_OWNER')")
    public ResponseEntity<List<VenueReservationDTO>> getAllVenueOwnerReservations() {
        try {
            List<VenueReservationDTO> allReservations =
                    venueReservationService.getAllReservationsForVenueOwner();
            return ResponseEntity.ok(allReservations);
        } catch (Exception e) {
            System.err.println("Error getting all venue owner reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
