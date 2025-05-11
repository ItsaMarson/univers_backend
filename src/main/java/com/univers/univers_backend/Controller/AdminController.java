/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.CreateUserDTO;
import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.EditUserDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Service.DepartmentService;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.Service.VenueService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrative APIs")
public class AdminController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final VenueService venueService;

    public AdminController(
            UserService userService,
            DepartmentService departmentService,
            VenueService venueService) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.venueService = venueService;
    }

    @Operation(summary = "Create user", description = "Creates a new user account")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Email already in use"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<String>> createUser(
            @Valid @RequestBody CreateUserDTO request) {
        try {
            String responseMessage = userService.createUser(request);
            if ("Email already in use".equals(responseMessage)) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(), "Email already in use"));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("User created successfully", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating user"));
        }
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all users")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Users retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        try {
            List<UserDTO> users = userService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving users"));
        }
    }

    @Operation(
            summary = "Edit user as admin",
            description = "Updates a user's profile with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping(value = "/users/{userId}")
    public ResponseEntity<ApiResponse<String>> editUserAsAdmin(
            @PathVariable UUID userId,
            @RequestPart("userDTO") EditUserDTO EditUserDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            String responseMessage = userService.editUserAsAdmin(userId, EditUserDTO, imageFile);
            if ("User does not exist".equals(responseMessage)
                    || "Invalid department Id".equals(responseMessage)
                    || responseMessage.startsWith("Failed to update profile image")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid input",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("User updated successfully", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while updating user"));
        }
    }

    @Operation(summary = "Deactivate user", description = "Deactivates a user account")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User deactivated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "User not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deactivateUser(@PathVariable UUID userId) {
        try {
            String responseMessage = userService.deactivateUser(userId);
            if ("User not found".equals(responseMessage)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "User not found"));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("User deactivated successfully", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while deactivating user"));
        }
    }

    @Operation(
            summary = "Activate user",
            description = "Activates a previously deactivated user account")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User activated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "User not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> activateUser(@PathVariable UUID userId) {
        try {
            String responseMessage = userService.activateUser(userId);
            if ("User not found".equals(responseMessage)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "User not found"));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("User activated successfully", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while activating user"));
        }
    }

    @Operation(
            summary = "Get department by ID",
            description = "Retrieves a specific department by its ID")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Department retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Department not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/department/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentById(@PathVariable UUID id) {
        try {
            DepartmentDTO departmentDTO = departmentService.getDepartmentDtoByPublicId(id);
            return ResponseEntity.ok(ApiResponse.success(departmentDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Department not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving department"));
        }
    }

    @Operation(summary = "Add department", description = "Creates a new department")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Department created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid department head"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Department already exists"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<String>> addDepartment(
            @RequestBody DepartmentDTO departmentDTO) {
        try {
            String response = departmentService.addDepartment(departmentDTO);
            if (response.contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                ApiResponse.error(
                                        HttpStatus.CONFLICT.value(),
                                        "Department already exists",
                                        response));
            }
            if (response.contains("Invalid department head")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid department head",
                                        response));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Department created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while creating department"));
        }
    }

    @Operation(summary = "Update department", description = "Updates an existing department")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Department updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid department head"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Department not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/department/{id}")
    public ResponseEntity<ApiResponse<String>> updateDepartment(
            @PathVariable UUID id, @RequestBody DepartmentDTO departmentDTO) {
        try {
            String response = departmentService.updateDepartment(id, departmentDTO);
            if (response.contains("does not exist")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                ApiResponse.error(
                                        HttpStatus.NOT_FOUND.value(),
                                        "Department not found",
                                        response));
            }
            if (response.contains("Invalid department head")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid department head",
                                        response));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Department updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while updating department"));
        }
    }

    @Operation(summary = "Delete department", description = "Deletes an existing department")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Department deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Department not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/department/{id}")
    public ResponseEntity<ApiResponse<String>> deleteDepartment(@PathVariable UUID id) {
        try {
            String response = departmentService.deleteDepartment(id);
            if (response.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                ApiResponse.error(
                                        HttpStatus.NOT_FOUND.value(),
                                        "Department not found",
                                        response));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Department deleted successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while deleting department"));
        }
    }

    @Operation(
            summary = "Assign department head",
            description = "Assigns a user as the head of a department")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Department head assigned successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/department/{departmentId}/assignHead/{userId}")
    public ResponseEntity<ApiResponse<String>> assignDepartmentHead(
            @PathVariable UUID departmentId, @PathVariable UUID userId) {
        try {
            String response = departmentService.assignDepartmentHead(departmentId, userId);
            return ResponseEntity.ok(
                    ApiResponse.success("Department head assigned successfully", response));
        } catch (RuntimeException e) {
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
                                    "An unexpected error occurred while assigning department"
                                            + " head"));
        }
    }

    @Operation(summary = "Add venue", description = "Creates a new venue with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "Venue created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PostMapping("/venues")
    public ResponseEntity<ApiResponse<VenueDTO>> addVenue(
            @RequestPart("venue") VenueDTO venueDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            VenueDTO newVenue = venueService.addVenue(venueDTO, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Venue created successfully", newVenue));
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
                                    "An unexpected error occurred while creating venue"));
        }
    }

    @Operation(
            summary = "Update venue",
            description = "Updates an existing venue with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Venue updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Venue not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<VenueDTO>> updateVenue(
            @PathVariable UUID venueId,
            @RequestPart("venue") VenueDTO venueDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            VenueDTO updatedVenue = venueService.updateVenue(venueId, venueDTO, imageFile);
            return ResponseEntity.ok(
                    ApiResponse.success("Venue updated successfully", updatedVenue));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Venue not found",
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
                                    "An unexpected error occurred while updating venue"));
        }
    }

    @Operation(summary = "Delete venue", description = "Deletes an existing venue")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Venue deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Venue not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Venue is associated with existing events"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/venues/{venueId}")
    public ResponseEntity<ApiResponse<String>> deleteVenue(@PathVariable UUID venueId) {
        try {
            venueService.deleteVenue(venueId);
            return ResponseEntity.ok(ApiResponse.success("Venue deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Venue not found",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.CONFLICT.value(),
                                    "Could not delete venue. It might be associated with existing"
                                        + " events or an error occurred during image deletion"));
        }
    }
}
