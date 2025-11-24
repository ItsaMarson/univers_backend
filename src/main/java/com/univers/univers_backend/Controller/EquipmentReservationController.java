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
import java.util.UUID;
import java.util.stream.Collectors;
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
        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getOwnEquipmentReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
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
        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(reservations));
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
        EquipmentReservationDTO reservation =
                equipmentReservationService.getReservationByPublicId(reservationId);
        return ResponseEntity.ok(ApiResponse.success(reservation));
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
        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getReservationsByEventPublicId(eventId);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @Operation(
            summary = "Approve reservations",
            description =
                    "Approves one or more equipment reservations with optional remarks. Can be used"
                            + " for both single and multiple reservations.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations processed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/approve")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<Map<UUID, String>>> approveReservations(
            @RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<String> reservationIds = (List<String>) payload.get("reservationIds");
        String remarks = (String) payload.getOrDefault("remarks", "");

        if (reservationIds == null || reservationIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    "Reservation IDs list cannot be null or empty."));
        }

        List<UUID> reservationUuids =
                reservationIds.stream().map(UUID::fromString).collect(Collectors.toList());
        Map<UUID, String> results =
                equipmentReservationService.bulkApproveReservations(reservationUuids, remarks);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation(s) approved successfully", results));
    }

    @Operation(
            summary = "Reject reservations",
            description =
                    "Rejects one or more equipment reservations with required remarks. Can be used"
                            + " for both single and multiple reservations.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations processed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request or missing remarks"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/reject")
    @PreAuthorize("hasAuthority('EQUIPMENT_OWNER')")
    public ResponseEntity<ApiResponse<Map<UUID, String>>> rejectReservations(
            @RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<String> reservationIds = (List<String>) payload.get("reservationIds");
        String remarks = (String) payload.get("remarks");

        if (reservationIds == null || reservationIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    "Reservation IDs list cannot be null or empty."));
        }

        if (remarks == null || remarks.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Rejection remarks are required"));
        }

        List<UUID> reservationUuids =
                reservationIds.stream().map(UUID::fromString).collect(Collectors.toList());
        Map<UUID, String> results =
                equipmentReservationService.bulkRejectReservations(reservationUuids, remarks);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation(s) rejected successfully", results));
    }

    @Operation(
            summary = "Cancel reservations",
            description =
                    "Cancels one or more equipment reservations. Can be used for both single and"
                            + " multiple reservations.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reservations processed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<UUID, String>>> cancelReservations(
            @RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<String> reservationIds = (List<String>) payload.get("reservationIds");

        if (reservationIds == null || reservationIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    "Reservation IDs list cannot be null or empty."));
        }

        List<UUID> reservationUuids =
                reservationIds.stream().map(UUID::fromString).collect(Collectors.toList());
        Map<UUID, String> results =
                equipmentReservationService.bulkCancelReservations(reservationUuids);

        return ResponseEntity.ok(
                ApiResponse.success("Reservation(s) cancelled successfully", results));
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
        List<EquipmentApprovalDTO> approvals =
                equipmentReservationService.getAllApprovalsForReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success(approvals));
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
        List<EquipmentReservationDTO> pendingReservations =
                equipmentReservationService.getPendingReservationsForEquipmentOwner();
        return ResponseEntity.ok(ApiResponse.success(pendingReservations));
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
        List<EquipmentReservationDTO> allReservations =
                equipmentReservationService.getAllReservationsForEquipmentOwner();
        return ResponseEntity.ok(ApiResponse.success(allReservations));
    }
}
