/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.Service.EventApprovalService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event-approval")
@Tag(name = "Event Approval", description = "APIs for managing event approvals")
public class EventApprovalController {

    private final EventApprovalService eventApprovalService;

    public EventApprovalController(EventApprovalService eventApprovalService) {
        this.eventApprovalService = eventApprovalService;
    }

    @Operation(summary = "Approve event", description = "Approves an event with optional remarks")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event approved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{eventId}/approve")
    public ResponseEntity<ApiResponse<String>> approveEvent(
            @PathVariable UUID eventId, @RequestBody Map<String, String> payload) {
        try {
            String remarks = payload.get("remarks");
            if (remarks == null) {
                remarks = "";
            }

            String responseMessage = eventApprovalService.approveEvent(eventId, remarks);

            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Event approved successfully", responseMessage));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred during event approval"));
        }
    }

    @Operation(summary = "Reject event", description = "Rejects an event with required remarks")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Event rejected successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request or missing remarks"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Event not found"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/{eventId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectEvent(
            @PathVariable UUID eventId, @RequestBody Map<String, String> payload) {
        try {
            String remarks = payload.get("remarks");
            if (remarks == null || remarks.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Rejection remarks are required"));
            }

            String responseMessage = eventApprovalService.rejectEvent(eventId, remarks);

            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest()
                        .body(
                                ApiResponse.error(
                                        HttpStatus.BAD_REQUEST.value(),
                                        "Invalid request",
                                        responseMessage));
            }
            return ResponseEntity.ok(
                    ApiResponse.success("Event rejected successfully", responseMessage));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.NOT_FOUND.value(),
                                    "Event not found",
                                    e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred during event rejection"));
        }
    }

    @Operation(
            summary = "Get event approvals",
            description = "Retrieves all approvals for a specific event")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Approvals retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<List<EventApprovalDTO>>> getAllApprovalsOfEvent(
            @PathVariable UUID eventId) {
        try {
            List<EventApprovalDTO> approvals = eventApprovalService.getAllApprovalsOfEvent(eventId);
            return ResponseEntity.ok(ApiResponse.success(approvals));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Invalid request",
                                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "An unexpected error occurred while retrieving event"
                                            + " approvals"));
        }
    }
}
