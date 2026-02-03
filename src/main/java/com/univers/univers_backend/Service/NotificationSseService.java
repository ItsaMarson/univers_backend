/* (C)2026 */
package com.univers.univers_backend.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    public NotificationSseService() {
        // Start heartbeat ping every 30 seconds to keep connections alive
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void sendHeartbeat() {
        log.trace("Sending SSE heartbeat to all active connections");
        emitters.forEach(
                (email, userEmitters) -> {
                    for (SseEmitter emitter : userEmitters) {
                        try {
                            emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
                        } catch (Exception e) {
                            removeEmitter(email, emitter);
                        }
                    }
                });
    }

    /**
     * Registers a new SSE emitter for a user. Supports multiple connections (tabs) per user.
     *
     * @param email The user's email/username
     * @return The created SseEmitter
     */
    public SseEmitter subscribe(String email) {
        // Create emitter with 30 minute timeout
        SseEmitter emitter = new SseEmitter(1800000L);

        emitters.computeIfAbsent(email, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(email, emitter));
        emitter.onTimeout(() -> removeEmitter(email, emitter));
        emitter.onError((e) -> removeEmitter(email, emitter));

        log.info(
                "User {} subscribed to SSE notifications. Active connections: {}",
                email,
                emitters.get(email).size());

        // Send an initial connect event to verify connection
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            log.error("Error sending initial connect event to {}", email, e);
            removeEmitter(email, emitter);
        }

        return emitter;
    }

    /**
     * Sends a notification to all active SSE connections for a specific user.
     *
     * @param email   The user's email
     * @param payload The data to send
     */
    public void sendToUser(String email, Object payload) {
        Set<SseEmitter> userEmitters = emitters.get(email);
        if (userEmitters != null) {
            log.debug(
                    "Sending SSE notification to user {}. Emitters: {}",
                    email,
                    userEmitters.size());
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(payload));
                } catch (IOException e) {
                    log.warn("Failed to send SSE to user {}, removing dead emitter", email);
                    removeEmitter(email, emitter);
                }
            }
        }
    }

    /**
     * Sends a notification to all connected users (broadcast).
     *
     * @param payload The data to send
     */
    public void broadcast(Object payload) {
        log.debug("Broadcasting SSE notification to all users");
        emitters.forEach(
                (email, userEmitters) -> {
                    for (SseEmitter emitter : userEmitters) {
                        try {
                            emitter.send(SseEmitter.event().name("notification").data(payload));
                        } catch (IOException e) {
                            removeEmitter(email, emitter);
                        }
                    }
                });
    }

    private void removeEmitter(String email, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(email);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(email);
            }
        }
    }
}
