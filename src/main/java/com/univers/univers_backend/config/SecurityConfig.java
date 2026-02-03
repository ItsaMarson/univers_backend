/* (C)2025-2026 */
package com.univers.univers_backend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${cors.allowed.origin:#{null}}")
    private String allowedOrigin;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter, UserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Only configure CORS if allowedOrigin is set (for development with separate frontend)
        if (allowedOrigin != null && !allowedOrigin.isEmpty()) {
            http.cors(
                    cors ->
                            cors.configurationSource(
                                    request -> {
                                        CorsConfiguration config = new CorsConfiguration();
                                        config.setAllowedOrigins(List.of(allowedOrigin));
                                        config.setAllowedMethods(
                                                List.of(
                                                        "GET", "POST", "PATCH", "DELETE", "PUT",
                                                        "OPTIONS", "HEAD"));
                                        config.setAllowedHeaders(
                                                List.of(
                                                        "Authorization",
                                                        "Content-Type",
                                                        "X-Requested-With",
                                                        "accept",
                                                        "Origin",
                                                        "Access-Control-Request-Method",
                                                        "Access-Control-Request-Headers"));
                                        config.setAllowCredentials(true);
                                        config.setExposedHeaders(List.of("Set-Cookie"));
                                        return config;
                                    }));
        } else {
            // Disable CORS when frontend and backend are served from same origin
            http.cors(AbstractHttpConfigurer::disable);
        }

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                // Allow access to static resources (frontend)
                                                "/",
                                                "/index.html",
                                                "/assets/**",
                                                "/*.js",
                                                "/*.css",
                                                "/*.ico",
                                                "/*.png",
                                                "/*.json",
                                                "/*.webp",
                                                "/*.svg",
                                                // Allow Swagger/OpenAPI documentation (static
                                                // resources, no /api prefix)
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                // Also allow with /api prefix for API docs
                                                // endpoints
                                                "/api/v3/api-docs/**",
                                                // Allow access to public API endpoints
                                                "/api/auth/register",
                                                "/api/auth/login",
                                                "/api/auth/verify-email",
                                                "/api/auth/resend-code",
                                                "/api/auth/logout",
                                                "/api/auth/forgot-password",
                                                "/api/auth/reset-password",
                                                "/api/auth/verify-reset-code",
                                                "/api/auth/me",
                                                "/api/auth/refresh",
                                                "/api/departments",
                                                "/api/files/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/admin/**",
                                                "/api/admin/users/**",
                                                "/api/admin/activity-logs/**")
                                        .hasAuthority("SUPER_ADMIN")
                                        .requestMatchers("/api/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
