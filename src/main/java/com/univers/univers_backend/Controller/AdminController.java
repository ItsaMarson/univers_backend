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
        String responseMessage = userService.createUser(request);
        if ("Email already in use".equals(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(), "Email already in use"));
        }
        return ResponseEntity.ok(ApiResponse.success("User created successfully", responseMessage));
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
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
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
            @RequestPart("userDTO") @Valid EditUserDTO EditUserDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
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
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", responseMessage));
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
        String responseMessage = userService.activateUser(userId);
        if ("User not found".equals(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "User not found"));
        }
        return ResponseEntity.ok(
                ApiResponse.success("User activated successfully", responseMessage));
    }

    @Operation(
            summary = "Bulk deactivate users",
            description =
                    "Deactivates multiple user accounts based on a list of public IDs. Returns a"
                            + " list of status messages for each user.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description =
                                "Bulk deactivation process completed. See response body for status"
                                        + " of each user."),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input provided (e.g., empty list of user IDs).",
                        content =
                                @io.swagger.v3.oas.annotations.media.Content(
                                        schema =
                                                @io.swagger.v3.oas.annotations.media.Schema(
                                                        implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error during bulk deactivation.",
                        content =
                                @io.swagger.v3.oas.annotations.media.Content(
                                        schema =
                                                @io.swagger.v3.oas.annotations.media.Schema(
                                                        implementation = ApiResponse.class)))
            })
    @PatchMapping("/users")
    public ResponseEntity<ApiResponse<List<String>>> bulkDeactivateUsers(
            @RequestBody List<UUID> userPublicIds) {
        List<String> results = userService.bulkDeactivateUsers(userPublicIds);

        // If the list of IDs was empty or null, the service returns a list like: ["User ID list
        // cannot be null or empty."]
        // In this specific scenario, it's more appropriate to return a 400 Bad Request.
        if (results.size() == 1 && "User ID list cannot be null or empty.".equals(results.get(0))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), results.get(0)));
        }

        return ResponseEntity.ok(
                ApiResponse.success("Bulk deactivation process completed.", results));
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
        DepartmentDTO departmentDTO = departmentService.getDepartmentDtoByPublicId(id);
        return ResponseEntity.ok(ApiResponse.success(departmentDTO));
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
            @Valid @RequestBody DepartmentDTO departmentDTO) {
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
            @PathVariable UUID id, @Valid @RequestBody DepartmentDTO departmentDTO) {
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
        return ResponseEntity.ok(ApiResponse.success("Department updated successfully", response));
    }

    @Operation(
            summary = "Bulk delete departments",
            description =
                    "Deletes multiple departments by their public IDs. "
                            + "Requires a List of UUIDs in the request body.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description =
                                "Bulk delete operation processed. See response body for detailed"
                                        + " status of each department."),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description =
                                "Invalid input, e.g., department ID list cannot be null or empty."),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error during bulk deletion process.")
            })
    @DeleteMapping("/departments")
    public ResponseEntity<ApiResponse<List<String>>> bulkDeleteDepartments(
            @RequestBody List<UUID> departmentPublicIds) {
        if (departmentPublicIds == null || departmentPublicIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Department ID list cannot be null or empty."));
        }
        List<String> results = departmentService.deleteDepartments(departmentPublicIds);
        return ResponseEntity.ok(ApiResponse.success("Bulk delete operation processed.", results));
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
        String response = departmentService.assignDepartmentHead(departmentId, userId);
        return ResponseEntity.ok(
                ApiResponse.success("Department head assigned successfully", response));
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
            @Valid @RequestPart("venue") VenueDTO venueDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        VenueDTO newVenue = venueService.addVenue(venueDTO, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue created successfully", newVenue));
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
            @Valid @RequestPart("venue") VenueDTO venueDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        VenueDTO updatedVenue = venueService.updateVenue(venueId, venueDTO, imageFile);
        return ResponseEntity.ok(ApiResponse.success("Venue updated successfully", updatedVenue));
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
        venueService.deleteVenue(venueId);
        return ResponseEntity.ok(ApiResponse.success("Venue deleted successfully"));
    }

    @Operation(
            summary = "Bulk delete venues",
            description = "Deletes multiple venues by their public IDs")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Venues deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Some venues were not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "Some venues could not be deleted due to existing events"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/venues/bulk")
    public ResponseEntity<ApiResponse<String>> bulkDeleteVenues(@RequestBody List<UUID> venueIds) {
        venueService.bulkDeleteVenues(venueIds);
        return ResponseEntity.ok(ApiResponse.success("Venues deleted successfully"));
    }
}
