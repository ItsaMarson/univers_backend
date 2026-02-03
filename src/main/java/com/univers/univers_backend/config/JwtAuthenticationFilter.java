/* (C)2025 */
package com.univers.univers_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        Optional<Cookie> accessTokenCookie = Optional.empty();
        Optional<Cookie> refreshTokenCookie = Optional.empty();

        if (cookies != null) {
            accessTokenCookie =
                    Arrays.stream(cookies)
                            .filter(cookie -> "access_token".equals(cookie.getName()))
                            .findFirst();
            refreshTokenCookie =
                    Arrays.stream(cookies)
                            .filter(cookie -> "refresh_token".equals(cookie.getName()))
                            .findFirst();
        }

        if (accessTokenCookie.isPresent()) {
            String token = accessTokenCookie.get().getValue();
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!(userDetails instanceof com.univers.univers_backend.Entity.User)) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write("{\"error\": \"User details configuration error.\"}");
                    return;
                }
                var auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                if (refreshTokenCookie.isPresent()) {
                    String rt = refreshTokenCookie.get().getValue();
                    if (jwtUtil.validateToken(rt)) {
                        String username = jwtUtil.extractUsername(rt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (!(userDetails instanceof com.univers.univers_backend.Entity.User)) {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\": \"User details configuration error.\"}");
                            return;
                        }

                        String newAccessToken =
                                jwtUtil.generateAccessToken(userDetails);
                        Cookie newAccessTokenCookie = new Cookie("access_token", newAccessToken);
                        newAccessTokenCookie.setHttpOnly(true);
                        newAccessTokenCookie.setPath("/");
                        newAccessTokenCookie.setMaxAge(
                                (int) (jwtUtil.ACCESS_TOKEN_EXPIRATION / 1000));
                        response.addCookie(newAccessTokenCookie);
                        var auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } else if (refreshTokenCookie.isPresent()) {
            String rt = refreshTokenCookie.get().getValue();
            if (jwtUtil.validateToken(rt)) {
                String username = jwtUtil.extractUsername(rt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!(userDetails instanceof com.univers.univers_backend.Entity.User)) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write("{\"error\": \"User details configuration error.\"}");
                    return;
                }

                String newAccessToken = jwtUtil.generateAccessToken(userDetails);
                Cookie newAccessTokenCookie = new Cookie("access_token", newAccessToken);
                newAccessTokenCookie.setHttpOnly(true);
                newAccessTokenCookie.setPath("/");
                newAccessTokenCookie.setMaxAge((int) (jwtUtil.ACCESS_TOKEN_EXPIRATION / 1000));
                response.addCookie(newAccessTokenCookie);

                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
                Cookie newRefreshTokenCookie = new Cookie("refresh_token", newRefreshToken);
                newRefreshTokenCookie.setHttpOnly(true);
                newRefreshTokenCookie.setPath("/");
                newRefreshTokenCookie.setMaxAge((int) (jwtUtil.REFRESH_TOKEN_EXPIRATION / 1000));
                response.addCookie(newRefreshTokenCookie);

                var auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
