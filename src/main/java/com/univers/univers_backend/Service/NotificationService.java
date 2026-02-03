/* (C)2025-2026 */
package com.univers.univers_backend.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univers.univers_backend.Entity.Notification;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.NotificationRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationSseService sseService;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationSseService sseService,
            ObjectMapper objectMapper,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.sseService = sseService;
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Sends a notification message as JSON to a specific user AND persists it.
     *
     * @param username    The username (e.g., email) of the user to notify.
     * @param destination The specific queue/topic suffix (ignored in SSE, kept for signature compatibility).
     * @param payload     The notification data (e.g., a Map or a DTO) to be sent as JSON.
     */
    @Transactional
    public void notifyUser(String username, String destination, Object payload) {
        String jsonPayload = null;

        try {
            jsonPayload = objectMapper.writeValueAsString(payload);

            sseService.sendToUser(username, jsonPayload);
            log.info("Sent JSON notification via SSE to user '{}'", username);
            log.debug("Payload: {}", jsonPayload);

            Optional<User> recipientOpt = userRepository.findByEmail(username);
            if (recipientOpt.isPresent()) {
                Notification notification = new Notification();
                notification.setRecipient(recipientOpt.get());
                notification.setMessage(
                        jsonPayload); // The raw JSON payload is stored as the message

                if (payload instanceof Map) {
                    Map<?, ?> payloadMap = (Map<?, ?>) payload;
                    String entityType = null;
                    UUID relatedPublicId = null;
                    UUID eventPublicId = null;

                    // Attempt to parse a specific eventPublicId if provided
                    if (payloadMap.containsKey("eventPublicId")) {
                        eventPublicId =
                                parseUUIDFromPayload(
                                        payloadMap.get("eventPublicId"), "eventPublicId", username);
                    }

                    // Determine related entity and its public ID
                    if (payloadMap.containsKey("venueReservationId")) {
                        entityType = "VENUE_RESERVATION";
                        relatedPublicId =
                                parseUUIDFromPayload(
                                        payloadMap.get("venueReservationId"),
                                        "venueReservationId",
                                        username);
                        // If eventPublicId wasn't set directly, try to get it from "eventId" for
                        // compatibility
                        if (eventPublicId == null && payloadMap.containsKey("eventId")) {
                            eventPublicId =
                                    parseUUIDFromPayload(
                                            payloadMap.get("eventId"), "eventId", username);
                        }
                    } else if (payloadMap.containsKey("equipmentReservationId")) {
                        entityType = "EQUIPMENT_RESERVATION";
                        relatedPublicId =
                                parseUUIDFromPayload(
                                        payloadMap.get("equipmentReservationId"),
                                        "equipmentReservationId",
                                        username);
                        if (eventPublicId == null && payloadMap.containsKey("eventId")) {
                            eventPublicId =
                                    parseUUIDFromPayload(
                                            payloadMap.get("eventId"), "eventId", username);
                        }
                    } else if (payloadMap.containsKey("eventId")) {
                        // If "eventId" is the primary identifier, it's both the event and the
                        // related entity
                        UUID parsedEventId =
                                parseUUIDFromPayload(
                                        payloadMap.get("eventId"), "eventId", username);
                        if (eventPublicId == null) {
                            eventPublicId = parsedEventId;
                        }
                        // Only set as related if no other more specific related entity was found
                        if (relatedPublicId == null) {
                            entityType = "EVENT";
                            relatedPublicId = parsedEventId;
                        }
                    }

                    // Allow direct override from payload if provided
                    if (payloadMap.containsKey("relatedEntityType")) {
                        entityType = (String) payloadMap.get("relatedEntityType");
                    }
                    if (payloadMap.containsKey("relatedEntityPublicId")) {
                        relatedPublicId =
                                parseUUIDFromPayload(
                                        payloadMap.get("relatedEntityPublicId"),
                                        "relatedEntityPublicId",
                                        username);
                    }

                    notification.setEventPublicId(eventPublicId);
                    notification.setRelatedEntityType(entityType);
                    notification.setRelatedEntityPublicId(relatedPublicId);
                }

                notificationRepository.save(notification);
                log.info(
                        "Persisted notification for user '{}' (Message snippet: {}, EventPublicId:"
                                + " {}, RelatedEntityType: {}, RelatedEntityPublicId: {})",
                        username,
                        notification
                                .getMessage()
                                .substring(
                                        0,
                                        Math.min(
                                                notification.getMessage().length(),
                                                50)), // Log a snippet
                        notification.getEventPublicId(),
                        notification.getRelatedEntityType(),
                        notification.getRelatedEntityPublicId());
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

    private UUID parseUUIDFromPayload(Object value, String keyName, String username) {
        if (value == null) {
            log.warn("Payload key '{}' is null for user {}", keyName, username);
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Could not parse UUID for key '{}' from notification payload value '{}' (type:"
                            + " {}) for user {}",
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
            sseService.broadcast(jsonPayload);
            log.info("Sent JSON notification via SSE (broadcast)");
            log.debug("Payload: {}", jsonPayload);
        } catch (JsonProcessingException e) {
            log.error(
                    "Error converting payload to JSON for topic {}: {}",
                    destination,
                    e.getMessage());
        } catch (Exception e) {
            log.error("Error sending notification via SSE: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Notification createNotification(
            User recipient,
            String message,
            UUID eventPublicId,
            UUID relatedEntityPublicId,
            String relatedEntityType) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setEventPublicId(eventPublicId);
        notification.setRelatedEntityPublicId(eventPublicId);
        notification.setRelatedEntityType(relatedEntityType);
        // notification.setIsRead(false);
        // // isRead defaults to false, createdAt defaults to now, publicId is generated on
        // prePersist

        Notification savedNotification = notificationRepository.save(notification);
        log.info(
                "Persisted notification for user '{}' (RecipientId: {}, Message: {}, EventPublicId:"
                        + " {}, RelatedEntityPublicId: {}, RelatedEntityType: {})",
                recipient.getEmail(),
                recipient.getPublicId(), // Assuming User entity has getPublicId()
                message,
                eventPublicId,
                relatedEntityPublicId,
                relatedEntityType);

        // Also send an SSE notification
        Map<String, Object> ssePayload = new HashMap<>();
        ssePayload.put("publicId", savedNotification.getPublicId());
        ssePayload.put("recipientPublicId", recipient.getPublicId());

        // Wrap plain string message in an object to match frontend expectations
        Map<String, Object> messageWrapper = new HashMap<>();
        messageWrapper.put("message", message);
        ssePayload.put("message", messageWrapper);

        ssePayload.put("eventPublicId", eventPublicId);
        ssePayload.put("relatedEntityPublicId", relatedEntityPublicId);
        ssePayload.put("relatedEntityType", relatedEntityType);
        ssePayload.put("createdAt", savedNotification.getCreatedAt().toString());
        ssePayload.put("isRead", savedNotification.isRead());

        try {
            String jsonPayload = objectMapper.writeValueAsString(ssePayload);
            sseService.sendToUser(recipient.getEmail(), jsonPayload);
            log.info("Sent SSE notification to user '{}'", recipient.getEmail());
        } catch (JsonProcessingException e) {
            log.error(
                    "Error converting SSE payload to JSON for user {}: {}",
                    recipient.getEmail(),
                    e.getMessage());
        } catch (Exception e) {
            log.error(
                    "Error sending SSE notification for user {}: {}",
                    recipient.getEmail(),
                    e.getMessage(),
                    e);
        }
        return savedNotification;
    }
}
