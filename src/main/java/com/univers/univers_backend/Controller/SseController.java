/* (C)2026 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.Service.NotificationSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications/sse")
@Tag(name = "Notifications", description = "SSE Notification endpoints")
public class SseController {

    private final NotificationSseService sseService;

    public SseController(NotificationSseService sseService) {
        this.sseService = sseService;
    }

    @Operation(summary = "Subscribe to real-time notifications via SSE")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User must be authenticated");
        }

        String email = auth.getName();
        return sseService.subscribe(email);
    }
}
