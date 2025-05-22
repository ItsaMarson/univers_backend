/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentApprovalDTO;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.Service.EquipmentReservationService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment-reservations")
@Tag(name = "Equipment Reservations", description = "APIs for managing equipment reservations")
public class EquipmentReservationController {

    private final EquipmentReservationService equipmentReservationService;

    public EquipmentReservationController(EquipmentReservationService equipmentReservationService) {
        this.equipmentReservationService = equipmentReservationService;
    }

    @Operation(
            summary = "Create equipment reservations",
            description =
                    "Creates one or more new equipment reservations. Accepts a list of reservation"
                            + " requests.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Reservations created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request data"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>> createEquipmentReservation(
            @RequestBody List<EquipmentReservationDTO> reservationDTOs) {
        try {
            if (reservationDTOs == null || reservationDTOs.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        "Reservation list cannot be null or empty."));
            }
            List<EquipmentReservationDTO> createdReservations =
                    equipmentReservationService.createBulkEquipmentReservations(reservationDTOs);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(
                            ApiResponse.success(
                                    "Equipment reservations created successfully",
                                    createdReservations));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    e.getMessage()));
        } catch (Exception e) {
            // Log the exception details for server-side debugging
            // logger.error("Error during bulk equipment reservation: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating equipment"
                                            + " reservations.",
                                    e.getMessage())); // Optionally include e.getMessage() if
            // safe to expose
        }
    }

    @Operation(
            summary = "Get own reservations",
            description = "Retrieves all equipment reservations for the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>> getOwnReservations() {
        try {
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getOwnEquipmentReservations();
            return ResponseEntity.ok(ApiResponse.success(reservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving own"
                                            + " reservations"));
        }
    }

    @Operation(
            summary = "Get all reservations",
            description = "Retrieves all equipment reservations (admin only)")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>> getAllReservations() {
        try {
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getAllReservations();
            return ResponseEntity.ok(ApiResponse.success(reservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving all"
                                            + " reservations"));
        }
    }

    @Operation(
            summary = "Get reservation by ID",
            description = "Retrieves a specific equipment reservation by its ID")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservation retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EquipmentReservationDTO>> getReservationById(
            @PathVariable UUID reservationId) {
        try {
            EquipmentReservationDTO reservation =
                    equipmentReservationService.getReservationByPublicId(reservationId);
            return ResponseEntity.ok(ApiResponse.success(reservation));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving reservation"));
        }
    }

    @Operation(
            summary = "Get reservations by event ID",
            description = "Retrieves all equipment reservations for a specific event")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/event/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>> getReservationsByEventId(
            @PathVariable UUID eventId) {
        try {
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getReservationsByEventPublicId(eventId);
            return ResponseEntity.ok(ApiResponse.success(reservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving event"
                                            + " reservations"));
        }
    }

    @Operation(
            summary = "Approve reservation",
            description = "Approves an equipment reservation with optional remarks")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservation approved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{reservationId}/approve")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<String>> approveReservation(
            @PathVariable UUID reservationId, @RequestBody Map<String, String> payload) {
        try {
            String remarks = payload.getOrDefault("remarks", "");
            String responseMessage =
                    equipmentReservationService.approveReservation(reservationId, remarks);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Reservation approved successfully", responseMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
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
                                    "An unexpected error occurred while approving reservation"));
        }
    }

    @Operation(
            summary = "Reject reservation",
            description = "Rejects an equipment reservation with required remarks")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservation rejected successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request or missing remarks"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{reservationId}/reject")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<String>> rejectReservation(
            @PathVariable UUID reservationId, @RequestBody Map<String, String> payload) {
        try {
            String remarks = payload.get("remarks");
            if (remarks == null || remarks.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Rejection remarks are required"));
            }
            String responseMessage =
                    equipmentReservationService.rejectReservation(reservationId, remarks);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Reservation rejected successfully", responseMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
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
                                    "An unexpected error occurred while rejecting reservation"));
        }
    }

    @Operation(summary = "Cancel reservation", description = "Cancels an equipment reservation")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservation cancelled successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> cancelReservation(@PathVariable UUID reservationId) {
        try {
            String responseMessage = equipmentReservationService.cancelReservation(reservationId);
            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Reservation cancelled successfully", responseMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
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
                                    "An unexpected error occurred while cancelling reservation"));
        }
    }

    @Operation(summary = "Delete reservation", description = "Deletes an equipment reservation")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservation deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReservation(@PathVariable UUID reservationId) {
        try {
            equipmentReservationService.deleteReservation(reservationId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
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
                                    "An unexpected error occurred while deleting reservation"));
        }
    }

    @Operation(
            summary = "Get reservation approvals",
            description = "Retrieves all approvals for a specific reservation")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Approvals retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Reservation not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{reservationId}/approvals")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EquipmentApprovalDTO>>> getApprovalsForReservation(
            @PathVariable UUID reservationId) {
        try {
            List<EquipmentApprovalDTO> approvals =
                    equipmentReservationService.getAllApprovalsForReservation(reservationId);
            return ResponseEntity.ok(ApiResponse.success(approvals));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Reservation not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving approvals"));
        }
    }

    @Operation(
            summary = "Get pending equipment owner reservations",
            description = "Retrieves all pending reservations for the equipment owner")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/pending/equipment-owner")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>>
            getPendingEquipmentOwnerReservations() {
        try {
            List<EquipmentReservationDTO> pendingReservations =
                    equipmentReservationService.getPendingReservationsForEquipmentOwner();
            return ResponseEntity.ok(ApiResponse.success(pendingReservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving pending"
                                            + " reservations"));
        }
    }

    @Operation(
            summary = "Get all equipment owner reservations",
            description = "Retrieves all reservations for the equipment owner")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/all/equipment-owner")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<List<EquipmentReservationDTO>>>
            getAllEquipmentOwnerReservations() {
        try {
            List<EquipmentReservationDTO> allReservations =
                    equipmentReservationService.getAllReservationsForEquipmentOwner();
            return ResponseEntity.ok(ApiResponse.success(allReservations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving all"
                                            + " reservations"));
        }
    }
}
