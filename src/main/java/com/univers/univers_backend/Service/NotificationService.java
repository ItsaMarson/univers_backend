/* (C)2025 */
package com.univers.univers_backend.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univers.univers_backend.Entity.Notification;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.NotificationRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Sends a notification message as JSON to a specific user AND persists it.
     *
     * @param username    The username (e.g., email) of the user to notify.
     * @param destination The specific queue/topic suffix (e.g., "/queue/notifications").
     * @param payload     The notification data (e.g., a Map or a DTO) to be sent as JSON.
     */
    @Transactional
    public void notifyUser(String username, String destination, Object payload) {
        String userDestination = "/user/" + username + destination;
        String jsonPayload = null;

        try {
            jsonPayload = objectMapper.writeValueAsString(payload);

            messagingTemplate.convertAndSendToUser(username, destination, jsonPayload);
            log.info(
                    "Sent JSON notification via WebSocket to user '{}' at destination '{}'",
                    username,
                    userDestination);
            log.debug("Payload: {}", jsonPayload);

            Optional<User> recipientOpt = userRepository.findByEmail(username);
            if (recipientOpt.isPresent()) {
                Notification notification = new Notification();
                notification.setRecipient(recipientOpt.get());
                notification.setMessage(jsonPayload);

                if (payload instanceof Map) {
                    Map<?, ?> payloadMap = (Map<?, ?>) payload;
                    if (payloadMap.containsKey("eventId")) {
                        try {
                            notification.setRelatedEntityId(
                                    Long.parseLong(payloadMap.get("eventId").toString()));
                            notification.setRelatedEntityType(
                                    "Event"); // Assuming it's always Event for now
                        } catch (NumberFormatException e) {
                            log.warn(
                                    "Could not parse eventId from notification payload for user {}",
                                    username);
                        }
                    }
                    // Add similar checks for other potential related entities
                }

                notificationRepository.save(notification);
                log.info("Persisted notification for user '{}'", username);
            } else {
                log.warn("Could not find user with email '{}' to persist notification.", username);
            }

        } catch (JsonProcessingException e) {
            log.error("Error converting payload to JSON for user {}: {}", username, e.getMessage());
        } catch (Exception e) {
            // Catch broader exceptions for WebSocket sending or DB saving
            log.error(
                    "Error processing or sending notification for user {}: {}",
                    username,
                    e.getMessage(),
                    e);
        }
    }

    // (Optional) Update notifyTopic similarly if you need to persist topic messages
    // Note: Persisting topic messages might require a different strategy (e.g., linking to users
    // who *might* see it)
    // or storing them generically without a specific recipient.
    public void notifyTopic(String destination, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            messagingTemplate.convertAndSend(destination, jsonPayload);
            log.info("Sent JSON notification via WebSocket to topic '{}'", destination);
            log.debug("Payload: {}", jsonPayload);
            // Add persistence logic here if needed for topic messages
        } catch (JsonProcessingException e) {
            log.error(
                    "Error converting payload to JSON for topic {}: {}",
                    destination,
                    e.getMessage());
        } catch (Exception e) {
            log.error("Error sending notification to topic {}: {}", destination, e.getMessage(), e);
        }
    }
}
