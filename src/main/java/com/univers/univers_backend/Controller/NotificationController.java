/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.NotificationDTO;
import com.univers.univers_backend.Service.UserNotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final UserNotificationService userNotificationService;

    @Autowired
    public NotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getMyNotifications(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<NotificationDTO> notifications =
                userNotificationService.getNotificationsForCurrentUser(pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/count-unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = userNotificationService.getUnreadNotificationCountForCurrentUser();
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(@RequestBody List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        userNotificationService.markNotificationsAsRead(notificationIds);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        userNotificationService.markAllNotificationsAsRead();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(@RequestBody List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        userNotificationService.deleteNotifications(notificationIds);
        return ResponseEntity.noContent().build(); // Or ResponseEntity.ok()
    }
}
