/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.EquipmentInputDTO;
import com.univers.univers_backend.Service.EquipmentService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/equipments")
@Tag(name = "Equipment", description = "Equipment management APIs")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @Operation(
            summary = "Add new equipment",
            description = "Creates a new equipment with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Equipment created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EquipmentDTO>> addEquipment(
            @RequestParam("userId") String userId,
            @Valid @RequestPart("equipment") EquipmentInputDTO equipmentInputDTO,
            @RequestPart(name = "image", required = false) MultipartFile imageFile) {

        EquipmentDTO newEquipment =
                equipmentService.addEquipment(userId, equipmentInputDTO, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment created successfully", newEquipment));
    }

    @Operation(summary = "Get all equipments", description = "Retrieves a list of all equipments")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipments retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "204",
                        description = "No equipments found")
            })
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<EquipmentDTO>>> getAllEquipments() {
        List<EquipmentDTO> allEquipments = equipmentService.getAllEquipments();
        if (allEquipments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.success("No equipments found", allEquipments));
        }
        return ResponseEntity.ok(ApiResponse.success(allEquipments));
    }

    @Operation(
            summary = "Get equipments by owner",
            description = "Retrieves all equipments owned by a specific user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipments retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "204",
                        description = "No equipments found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid user ID"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<List<EquipmentDTO>>> getAllEquipmentsByOwner(
            @RequestParam String userId) {
        List<EquipmentDTO> allEquipmentsByOwner = equipmentService.getAllEquipmentsByOwner(userId);
        if (allEquipmentsByOwner.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(
                            ApiResponse.success(
                                    "No equipments found for this owner", allEquipmentsByOwner));
        }
        return ResponseEntity.ok(ApiResponse.success(allEquipmentsByOwner));
    }

    @Operation(
            summary = "Get equipment by ID",
            description = "Retrieves a specific equipment by its ID")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipment retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Equipment not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{equipmentId}")
    public ResponseEntity<ApiResponse<EquipmentDTO>> getEquipmentById(
            @PathVariable String equipmentId) {
        EquipmentDTO equipment = equipmentService.getEquipmentById(equipmentId);
        return ResponseEntity.ok(ApiResponse.success(equipment));
    }

    @Operation(
            summary = "Update equipment",
            description = "Updates an existing equipment with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipment updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Operation not allowed"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Equipment not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping(value = "/{equipmentId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EquipmentDTO>> updateEquipment(
            @PathVariable String equipmentId,
            @RequestParam("userId") String userId,
            @Valid @RequestPart("equipment") EquipmentInputDTO equipmentInputDTO,
            @RequestPart(name = "image", required = false) MultipartFile imageFile) {
        EquipmentDTO updatedEquipment =
                equipmentService.updateEquipment(equipmentId, userId, equipmentInputDTO, imageFile);
        return ResponseEntity.ok(
                ApiResponse.success("Equipment updated successfully", updatedEquipment));
    }

    @Operation(summary = "Delete equipment", description = "Deletes multiple equipments")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipments deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Operation not allowed"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Equipment not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Equipment is associated with other records"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteEquipments(
            @RequestBody Map<String, Object> payload, @RequestParam String userId) {
        @SuppressWarnings("unchecked")
        List<String> equipmentIds = (List<String>) payload.get("equipmentIds");

        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    "Equipment IDs list cannot be null or empty."));
        }

        List<UUID> equipmentUuids =
                equipmentIds.stream().map(UUID::fromString).collect(Collectors.toList());
        Map<String, String> results = equipmentService.bulkDeleteEquipments(equipmentUuids, userId);

        return ResponseEntity.ok(ApiResponse.success("Equipment(s) deleted successfully", results));
    }
}
