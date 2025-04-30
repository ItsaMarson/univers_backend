/* (C)2025 */
package com.univers.univers_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // Clients subscribe to destinations prefixed with "/topic" or "/queue"
        config.enableSimpleBroker("/topic", "/queue");
        // Messages sent from clients to destinations prefixed with "/app" will be
        // routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Use "/user" prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint that clients will connect to
        // "/ws" is a common choice
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "*"); // Allow connections from any origin (adjust for production)
    }
}
