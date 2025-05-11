/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EditUserDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    @GetMapping
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
            summary = "Update user profile",
            description = "Updates a user's profile with optional image")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User profile updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid input"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> updateUserProfile(
            @PathVariable UUID userId,
            @RequestPart("userDTO") EditUserDTO userDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            String responseMessage = userService.updateUserProfile(userId, userDTO, imageFile);
            if ("User does not exist".equals(responseMessage)
                    || "Invalid department Id".equals(responseMessage)) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid input",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("User profile updated successfully", responseMessage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while updating user profile"));
        }
    }

    @Operation(
            summary = "Get managed venue",
            description = "Retrieves the venue managed by a specific user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Managed venue retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "No managed venue found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{userId}/managed-venue")
    public ResponseEntity<ApiResponse<VenueDTO>> getManagedVenue(@PathVariable UUID userId) {
        try {
            VenueDTO managedVenue = userService.getManagedVenue(userId);
            if (managedVenue == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                                ApiResponse.error(
                                        HttpStatus.NOT_FOUND.value(), "No managed venue found"));
            }
            return ResponseEntity.ok(ApiResponse.success(managedVenue));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving managed venue"));
        }
    }

    @Operation(
            summary = "Get own events",
            description = "Retrieves all events created by the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Events retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "User not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/me/events")
    public ResponseEntity<ApiResponse<List<EventDTO>>> getOwnEvents() {
        try {
            List<EventDTO> events = userService.getOwnEvents();
            return ResponseEntity.ok(ApiResponse.success(events));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving events"));
        }
    }
}
