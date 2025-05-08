/* (C)2025 */
package com.univers.univers_backend.Service;

// Removed ObjectMapper import if no longer needed elsewhere
// import com.fasterxml.jackson.databind.ObjectMapper;
import com.univers.univers_backend.DTO.NotificationDTO;
import com.univers.univers_backend.Entity.Notification;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.NotificationRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.List;
import java.util.UUID; // Keep UUID import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // private final ObjectMapper objectMapper; // Removed ObjectMapper field

    public UserNotificationService(
            NotificationRepository notificationRepository, UserRepository userRepository
            // ObjectMapper objectMapper // Removed ObjectMapper parameter
            ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        // this.objectMapper = objectMapper; // Removed assignment
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
        // Pass the raw message string (which is stored in the DB) directly
        Object messageObject = notification.getMessage();

        // --- JSON Parsing Logic Removed ---

        return new NotificationDTO(
                notification.getPublicId(),
                notification.getEventPublicId(),
                messageObject, // Pass the raw message string
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getRelatedEntityPublicId(),
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
        // Assuming the original method existed and worked:
        return notificationRepository.countByRecipientAndIsReadFalseAndDeletedFalse(currentUser);
    }

    // --- Updated Methods Using UUID ---

    @Transactional
    public void markNotificationsAsRead(List<UUID> notificationPublicIds) { // Changed to List<UUID>
        User currentUser = getCurrentUser();
        if (!notificationPublicIds.isEmpty()) {
            notificationRepository.markAsReadByPublicIds(
                    notificationPublicIds, currentUser); // Use new method
        }
    }

    @Transactional
    public void markAllNotificationsAsReadForCurrentUser() {
        User currentUser = getCurrentUser();
        notificationRepository.markAllAsRead(currentUser); // This method was already correct
    }

    @Transactional
    public void deleteNotifications(List<UUID> notificationPublicIds) { // Changed to List<UUID>
        User currentUser = getCurrentUser();
        if (!notificationPublicIds.isEmpty()) {
            notificationRepository.markAsDeletedByPublicIds(
                    notificationPublicIds, currentUser); // Use new method (soft delete)
        }
    }

    @Transactional
    public void deleteAllNotificationsForCurrentUser() {
        User currentUser = getCurrentUser();
        notificationRepository.markAllAsDeletedForUser(currentUser); // Use new method (soft delete)
        log.info("Marked all active notifications as deleted for user {}", currentUser.getEmail());
    }
}
