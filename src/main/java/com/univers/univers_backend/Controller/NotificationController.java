/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.NotificationDTO;
import com.univers.univers_backend.Service.UserNotificationService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "APIs for managing user notifications")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    public NotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @Operation(
            summary = "Get notifications",
            description = "Retrieves paginated notifications for the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Notifications retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getMyNotifications(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<NotificationDTO> notifications =
                userNotificationService.getNotificationsForCurrentUser(pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @Operation(
            summary = "Get unread count",
            description = "Retrieves the count of unread notifications for the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Unread count retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/count-unread")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        long count = userNotificationService.getUnreadNotificationCountForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @Operation(
            summary = "Mark notifications as read",
            description = "Marks specified notifications as read")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Notifications marked as read successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestBody List<UUID> notificationPublicIds) {
        if (notificationPublicIds == null || notificationPublicIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Notification IDs cannot be empty"));
        }
        userNotificationService.markNotificationsAsRead(notificationPublicIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all notifications as read for the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "All notifications marked as read successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        userNotificationService.markAllNotificationsAsReadForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Delete notifications", description = "Deletes specified notifications")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Notifications deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid request"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteNotifications(
            @RequestBody List<UUID> notificationPublicIds) {
        if (notificationPublicIds == null || notificationPublicIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Notification IDs cannot be empty"));
        }
        userNotificationService.deleteNotifications(notificationPublicIds);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "Delete all notifications",
            description = "Deletes all notifications for the current user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "All notifications deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @DeleteMapping("/all")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications() {
        userNotificationService.deleteAllNotificationsForCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
