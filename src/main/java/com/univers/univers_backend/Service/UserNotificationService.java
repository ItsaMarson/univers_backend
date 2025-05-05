/* (C)2025 */
package com.univers.univers_backend.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univers.univers_backend.DTO.NotificationDTO;
import com.univers.univers_backend.Entity.Notification;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.NotificationRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationService {
    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserNotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    private User getCurrentUser() {
        String username =
                ((UserDetails)
                                SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal())
                        .getUsername();
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private NotificationDTO mapToDTO(Notification notification) {
        Object messageObject = null;
        try {
            // Parse the stored JSON string back into an Object (Map, List, etc.)
            messageObject = objectMapper.readValue(notification.getMessage(), Object.class);
        } catch (Exception e) {
            log.error(
                    "Failed to parse notification message JSON for notification id {}: {}",
                    notification.getId(),
                    e.getMessage());
            // Fallback: return the raw string or a custom error object if parsing fails
            messageObject =
                    Map.of(
                            "error",
                            "Failed to parse message content",
                            "rawMessage",
                            notification.getMessage());
        }

        return new NotificationDTO(
                notification.getId(),
                notification.getEventId(),
                messageObject, // Use the parsed object
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getRelatedEntityId(),
                notification.getRelatedEntityType());
    }

    public Page<NotificationDTO> getNotificationsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Notification> notifications =
                notificationRepository.findByRecipientAndDeletedFalseOrderByCreatedAtDesc(
                        currentUser, pageable);
        return notifications.map(this::mapToDTO);
    }

    public long getUnreadNotificationCountForCurrentUser() {
        User currentUser = getCurrentUser();
        return notificationRepository.countByRecipientAndIsReadFalseAndDeletedFalse(currentUser);
    }

    @Transactional
    public void markNotificationsAsRead(List<Long> notificationIds) {
        User currentUser = getCurrentUser();
        if (!notificationIds.isEmpty()) {
            notificationRepository.markAsRead(notificationIds, currentUser);
        }
    }

    @Transactional
    public void markAllNotificationsAsRead() {
        User currentUser = getCurrentUser();
        notificationRepository.markAllAsRead(currentUser);
    }

    @Transactional
    public void deleteNotifications(List<Long> notificationIds) {
        User currentUser = getCurrentUser();
        if (!notificationIds.isEmpty()) {
            notificationRepository.markAsDeleted(notificationIds, currentUser);
        }
    }
}
