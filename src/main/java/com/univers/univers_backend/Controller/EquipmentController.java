/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.EquipmentInputDTO;
import com.univers.univers_backend.Service.EquipmentService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.NoSuchElementException;
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
            @RequestPart("equipment") EquipmentInputDTO equipmentInputDTO,
            @RequestPart(name = "image", required = true) MultipartFile imageFile) {

        try {
            EquipmentDTO newEquipment =
                    equipmentService.addEquipment(userId, equipmentInputDTO, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Equipment created successfully", newEquipment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid input",
                                    e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating the equipment"));
        }
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
        try {
            List<EquipmentDTO> allEquipmentsByOwner =
                    equipmentService.getAllEquipmentsByOwner(userId);
            if (allEquipmentsByOwner.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(
                                ApiResponse.success(
                                        "No equipments found for this owner",
                                        allEquipmentsByOwner));
            }
            return ResponseEntity.ok(ApiResponse.success(allEquipmentsByOwner));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid user ID",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving equipments"));
        }
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
        try {
            EquipmentDTO equipment = equipmentService.getEquipmentById(equipmentId);
            return ResponseEntity.ok(ApiResponse.success(equipment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Equipment not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving the equipment"));
        }
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
            @RequestPart("equipment") EquipmentInputDTO equipmentInputDTO,
            @RequestPart(name = "image", required = false) MultipartFile imageFile) {
        try {
            EquipmentDTO updatedEquipment =
                    equipmentService.updateEquipment(
                            equipmentId, userId, equipmentInputDTO, imageFile);
            return ResponseEntity.ok(
                    ApiResponse.success("Equipment updated successfully", updatedEquipment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Equipment not found",
                                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.FORBIDDEN.value(),
                                    "Operation not allowed",
                                    e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while updating the equipment"));
        }
    }

    @Operation(summary = "Delete equipment", description = "Deletes an existing equipment")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Equipment deleted successfully"),
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
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<ApiResponse<String>> deleteEquipment(
            @PathVariable String equipmentId, @RequestParam String userId) {
        try {
            equipmentService.deleteEquipment(equipmentId, userId);
            return ResponseEntity.ok(ApiResponse.success("Equipment deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Equipment not found",
                                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.FORBIDDEN.value(),
                                    "Operation not allowed",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.CONFLICT.value(),
                                    "Could not delete equipment. It might be associated with other"
                                            + " records"));
        }
    }
}
