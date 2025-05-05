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
                    String entityType = null;
                    Long entityId = null;
                    Long eventId = null;

                    if (payloadMap.containsKey("eventId")) {
                        entityType = "EVENT";
                        entityId =
                                parseLongFromPayload(
                                        payloadMap.get("eventId"), "eventId", username);
                    } else if (payloadMap.containsKey("venueReservationId")) {
                        entityType = "VENUE_RESERVATION";
                        entityId =
                                parseLongFromPayload(
                                        payloadMap.get("venueReservationId"),
                                        "venueReservationId",
                                        username);
                    } else if (payloadMap.containsKey("equipmentReservationId")) {
                        entityType = "EQUIPMENT_RESERVATION";
                        entityId =
                                parseLongFromPayload(
                                        payloadMap.get("equipmentReservationId"),
                                        "equipmentReservationId",
                                        username);
                    }

                    notification.setRelatedEntityType(entityType);
                    notification.setRelatedEntityId(entityId);

                    if (payloadMap.containsKey("eventId")) {
                        eventId =
                                parseLongFromPayload(
                                        payloadMap.get("eventId"), "eventId", username);
                        notification.setEventId(eventId); // Set the dedicated eventId field
                    }
                }
                notificationRepository.save(notification);
                log.info(
                        "Persisted notification for user '{}' (EntityType: {}, EntityId: {})",
                        username,
                        notification.getRelatedEntityType(),
                        notification.getRelatedEntityId());
            } else {
                log.warn("Could not find user with email '{}' to persist notification.", username);
            }

        } catch (JsonProcessingException e) {
            log.error("Error converting payload to JSON for user {}: {}", username, e.getMessage());
        } catch (Exception e) {
            log.error(
                    "Error processing or sending notification for user {}: {}",
                    username,
                    e.getMessage(),
                    e);
        }
    }

    private Long parseLongFromPayload(Object value, String keyName, String username) {
        if (value == null) {
            log.warn("Payload key '{}' is null for user {}", keyName, username);
            return null;
        }
        try {
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            }
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn(
                    "Could not parse '{}' from notification payload value '{}' (type: {}) for user"
                            + " {}",
                    keyName,
                    value,
                    value.getClass().getSimpleName(),
                    username);
            return null;
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
