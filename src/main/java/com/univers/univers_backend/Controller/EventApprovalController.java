/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.ApprovalActionRequest;
import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Service.EventApprovalService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event-approval")
@Tag(name = "Event Approval", description = "APIs for managing event approvals")
public class EventApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(EventApprovalController.class);

    private final EventApprovalService eventApprovalService;

    public EventApprovalController(EventApprovalService eventApprovalService) {
        this.eventApprovalService = eventApprovalService;
    }

    @Operation(
            summary = "Process an approval action (approve/reject) for an event approval item",
            description =
                    "Allows an assigned approver to approve or reject a specific event approval"
                            + " task.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Approval action processed successfully",
                        content =
                                @io.swagger.v3.oas.annotations.media.Content(
                                        mediaType = "application/json",
                                        schema =
                                                @io.swagger.v3.oas.annotations.media.Schema(
                                                        implementation = EventApprovalDTO.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description =
                                "Invalid request (e.g., invalid status, item already processed)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "User not authenticated or not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "User not authorized to perform this action"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event approval item not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PutMapping("/{eventPublicId}/action")
    public ResponseEntity<ApiResponse<EventApprovalDTO>> processEventApprovalAction(
            @PathVariable UUID eventPublicId,
            @RequestBody ApprovalActionRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.UNAUTHORIZED.value(), "User not authenticated"));
        }

        try {
            Status statusEnum;
            try {
                statusEnum = Status.valueOf(request.status().toUpperCase());
                if (statusEnum != Status.APPROVED && statusEnum != Status.REJECTED) {
                    throw new IllegalArgumentException("Status must be APPROVED or REJECTED.");
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid status value. Must be APPROVED or REJECTED."));
            }

            EventApprovalDTO updatedDto =
                    eventApprovalService.processApprovalAction(
                            eventPublicId, statusEnum, request.remarks());
            return ResponseEntity.ok(ApiResponse.success(updatedDto));

        } catch (NoSuchElementException e) {
            logger.warn(
                    "Attempted to process non-existent event or approval for event {}: {}",
                    eventPublicId,
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Resource not found",
                                    e.getMessage()));
        } catch (SecurityException e) {
            logger.warn(
                    "Authorization failed for user {} on event {}: {}",
                    authentication.getName(),
                    eventPublicId,
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.FORBIDDEN.value(), "Forbidden", e.getMessage()));
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Invalid request for event {}: {}", eventPublicId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    e.getMessage()));
        } catch (UsernameNotFoundException e) {
            logger.warn(
                    "Authenticated user {} could not be resolved by the service: {}",
                    authentication.getName(),
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "User validation failed",
                                    e.getMessage()));
        } catch (Exception e) {
            logger.error(
                    "Unexpected error processing approval action for event {}: {}",
                    eventPublicId,
                    e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred. Please try again later."));
        }
    }

    @Operation(
            summary = "Get all approvals for a specific event",
            description =
                    "Retrieves a list of all approval statuses associated with a given event.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Approvals retrieved successfully",
                        content =
                                @io.swagger.v3.oas.annotations.media.Content(
                                        mediaType = "application/json",
                                        schema =
                                                @io.swagger.v3.oas.annotations.media.Schema(
                                                        implementation = List.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{eventPublicId}")
    public ResponseEntity<ApiResponse<List<EventApprovalDTO>>> getAllApprovalsForEvent(
            @PathVariable UUID eventPublicId) {
        try {
            List<EventApprovalDTO> approvals =
                    eventApprovalService.getAllApprovalsOfEvent(eventPublicId);
            return ResponseEntity.ok(ApiResponse.success(approvals));
        } catch (NoSuchElementException e) {
            logger.warn(
                    "Attempted to get approvals for non-existent event {}: {}",
                    eventPublicId,
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (Exception e) {
            logger.error(
                    "Unexpected error retrieving approvals for event {}: {}",
                    eventPublicId,
                    e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving event"
                                            + " approvals."));
        }
    }
}
