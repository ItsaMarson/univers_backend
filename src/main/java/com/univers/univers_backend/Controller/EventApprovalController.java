/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.Service.EventApprovalService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/event-approval")
public class EventApprovalController {

    private final EventApprovalService eventApprovalService;

    public EventApprovalController(EventApprovalService eventApprovalService) {
        this.eventApprovalService = eventApprovalService;
    }

    @PatchMapping("/{eventId}/approve")
    public ResponseEntity<String> approveEvent(
            @PathVariable UUID eventId, @RequestBody Map<String, String> payload) {

        String remarks = payload.get("remarks");
        if (remarks == null) {
            remarks = "";
        }

        try {
            String responseMessage = eventApprovalService.approveEvent(eventId, remarks);

            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest().body(responseMessage);
            }
            return ResponseEntity.ok(responseMessage);
        } catch (RuntimeException e) {
            System.err.println("Error during event approval: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred during approval.");
        }
    }

    @PatchMapping("/{eventId}/reject")
    public ResponseEntity<String> rejectEvent(
            @PathVariable UUID eventId, @RequestBody Map<String, String> payload) {

        String remarks = payload.get("remarks");
        if (remarks == null || remarks.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Rejection remarks are required.");
        }

        try {
            String responseMessage = eventApprovalService.rejectEvent(eventId, remarks);

            if (responseMessage.startsWith("Error:") || responseMessage.startsWith("Warning:")) {
                return ResponseEntity.badRequest().body(responseMessage);
            }
            return ResponseEntity.ok(responseMessage);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error during event rejection: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred during rejection.");
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getAllApprovalsOfEvent(@PathVariable UUID eventId) {

        try {
            List<EventApprovalDTO> approvals = eventApprovalService.getAllApprovalsOfEvent(eventId);
            return ResponseEntity.ok(approvals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
