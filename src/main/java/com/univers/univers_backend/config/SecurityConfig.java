/* (C)2025 */
package com.univers.univers_backend.config;

import java.util.List;
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
    private final UserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter, UserDetailsService userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(
                        cors ->
                                cors.configurationSource(
                                        request -> {
                                            CorsConfiguration config = new CorsConfiguration();
                                            config.setAllowedOrigins(
                                                    List.of("http://localhost:5173"));
                                            config.setAllowedMethods(
                                                    List.of(
                                                            "GET", "POST", "PATCH", "DELETE",
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
                                        }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/auth/register",
                                                "/auth/login",
                                                "/auth/verify-email",
                                                "/auth/resend-code",
                                                "/auth/logout",
                                                "/auth/forgot-password",
                                                "/auth/reset-password",
                                                "/auth/verify-reset-code",
                                                "/auth/me")
                                        .permitAll()
                                        .requestMatchers("/admin/**", "/admin/users/**")
                                        .hasAuthority("SUPER_ADMIN")
                                        .anyRequest()
                                        .authenticated())
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
