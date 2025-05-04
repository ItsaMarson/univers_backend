/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentApprovalDTO;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.Service.EquipmentReservationService;
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

@RestController
@RequestMapping("/equipment-reservations")
public class EquipmentReservationController {

    private final EquipmentReservationService equipmentReservationService;

    public EquipmentReservationController(EquipmentReservationService equipmentReservationService) {
        this.equipmentReservationService = equipmentReservationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createEquipmentReservation(
            @RequestPart("reservation") EquipmentReservationDTO reservationDTO) {
        try {
            EquipmentReservationDTO createdReservation =
                    equipmentReservationService.createEquipmentReservation(reservationDTO);
            return new ResponseEntity<>(createdReservation, HttpStatus.CREATED);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error creating equipment reservation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipmentReservationDTO>> getOwnReservations() {
        try {
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getOwnEquipmentReservations();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println("Error getting own equipment reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<EquipmentReservationDTO>> getAllReservations() {
        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReservationById(@PathVariable Long reservationId) {
        try {
            EquipmentReservationDTO reservation =
                    equipmentReservationService.getReservationById(reservationId);
            // Add authorization check if needed
            return ResponseEntity.ok(reservation);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error getting equipment reservation by ID "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReservationsByEventId(@PathVariable Long eventId) {
        try {
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getReservationsByEventId(eventId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            System.err.println(
                    "Error getting equipment reservations for event ID "
                            + eventId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{reservationId}/approve")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')") // Or other roles defined in service
    public ResponseEntity<String> approveReservation(
            @PathVariable Long reservationId, @RequestBody Map<String, String> payload) {
        String remarks = payload.getOrDefault("remarks", "");
        try {
            String responseMessage =
                    equipmentReservationService.approveReservation(reservationId, remarks);
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
                    "Error approving equipment reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Approval failed.");
        }
    }

    @PatchMapping("/{reservationId}/reject")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')") // Or other roles defined in service
    public ResponseEntity<String> rejectReservation(
            @PathVariable Long reservationId, @RequestBody Map<String, String> payload) {
        String remarks = payload.get("remarks");
        if (remarks == null || remarks.isBlank()) {
            return ResponseEntity.badRequest().body("Rejection remarks are required.");
        }
        try {
            String responseMessage =
                    equipmentReservationService.rejectReservation(reservationId, remarks);
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
                    "Error rejecting equipment reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Rejection failed.");
        }
    }

    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> cancelReservation(@PathVariable Long reservationId) {
        try {
            String responseMessage = equipmentReservationService.cancelReservation(reservationId);
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
                    "Error cancelling equipment reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Cancellation failed.");
        }
    }

    @DeleteMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteReservation(@PathVariable Long reservationId) {
        try {
            equipmentReservationService.deleteReservation(reservationId);
            return ResponseEntity.ok("Equipment reservation deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error deleting equipment reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deletion failed.");
        }
    }

    @GetMapping("/{reservationId}/approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getApprovalsForReservation(@PathVariable Long reservationId) {
        try {
            List<EquipmentApprovalDTO> approvals =
                    equipmentReservationService.getAllApprovalsForReservation(reservationId);
            return ResponseEntity.ok(approvals);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println(
                    "Error getting approvals for equipment reservation "
                            + reservationId
                            + ": "
                            + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get approvals.");
        }
    }

    @GetMapping("/pending/equipment-owner")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<List<EquipmentReservationDTO>> getPendingEquipmentOwnerReservations() {
        try {
            List<EquipmentReservationDTO> pendingReservations =
                    equipmentReservationService.getPendingReservationsForEquipmentOwner();
            return ResponseEntity.ok(pendingReservations);
        } catch (Exception e) {
            System.err.println(
                    "Error getting pending equipment owner reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/all/equipment-owner")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<List<EquipmentReservationDTO>> getAllEquipmentOwnerReservations() {
        try {
            List<EquipmentReservationDTO> allReservations =
                    equipmentReservationService.getAllReservationsForEquipmentOwner();
            return ResponseEntity.ok(allReservations);
        } catch (Exception e) {
            System.err.println("Error getting all equipment owner reservations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
}
