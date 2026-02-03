/* (C)2026 */
package com.univers.univers_backend.Controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications/sse")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String email = auth.getName();

        // Create emitter with 30 minute timeout
        SseEmitter emitter = new SseEmitter(1800000L);
        
        emitters.put(email, emitter);

        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError((e) -> emitters.remove(email));

        log.info("User {} subscribed to SSE notifications", email);

        // Send an initial connect event
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            log.error("Error sending initial connect event to {}", email, e);
            emitters.remove(email);
        }

        return emitter;
    }

    public static void sendNotification(String email, Object payload) {
        SseEmitter emitter = emitters.get(email);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException e) {
                log.error("Error sending SSE notification to {}", email, e);
                emitters.remove(email);
            }
        }
    }
}
