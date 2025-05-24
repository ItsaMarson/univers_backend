/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.BulkApprovalActionRequest;
import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Service.EventApprovalService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
            summary = "Process a bulk approval action (approve/reject) for multiple events",
            description =
                    "Allows an assigned approver to approve or reject multiple events at once. Can"
                            + " also be used for single events by providing a list with one ID.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Bulk approval action processed successfully",
                        content =
                                @io.swagger.v3.oas.annotations.media.Content(
                                        mediaType = "application/json",
                                        schema =
                                                @io.swagger.v3.oas.annotations.media.Schema(
                                                        implementation = List.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request (e.g., invalid status, empty event list)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "User not authenticated or not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "User not authorized to perform this action"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error or partial failure")
            })
    @PostMapping("/action")
    public ResponseEntity<ApiResponse<List<EventApprovalDTO>>> processBulkEventApprovalAction(
            @Valid @RequestBody BulkApprovalActionRequest request, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.UNAUTHORIZED.value(), "User not authenticated"));
        }

        if (request.status() != Status.APPROVED && request.status() != Status.REJECTED) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid status value. Must be APPROVED or REJECTED."));
        }

        if (request.eventPublicIds() == null || request.eventPublicIds().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Event ID list cannot be empty."));
        }

        try {
            List<EventApprovalDTO> results =
                    eventApprovalService.processBulkApprovalAction(
                            request.eventPublicIds(), request.status(), request.remarks());
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            logger.error(
                    "Error processing bulk approval action for user {}: {}",
                    authentication.getName(),
                    e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Error processing bulk action",
                                    e.getMessage()));
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
        } catch (java.util.NoSuchElementException e) {
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
