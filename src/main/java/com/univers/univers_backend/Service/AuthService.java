/* (C)2025-2026 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public void setAuthCookies(HttpServletResponse response, UserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        response.addHeader("Set-Cookie", createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", createRefreshTokenCookie(refreshToken).toString());
    }

    public void setAuthCookies(
            HttpServletResponse response, UserDetails userDetails, HttpServletRequest request) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        boolean isSecure = determineSecureFlag(request);
        response.addHeader("Set-Cookie", createAccessTokenCookie(accessToken, isSecure).toString());
        response.addHeader(
                "Set-Cookie", createRefreshTokenCookie(refreshToken, isSecure).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(
                "Set-Cookie", createDeletionCookie("access_token", sslEnabled).toString());
        response.addHeader(
                "Set-Cookie", createDeletionCookie("refresh_token", sslEnabled).toString());
    }

    public void clearAuthCookies(HttpServletResponse response, HttpServletRequest request) {
        boolean isSecure = determineSecureFlag(request);
        response.addHeader("Set-Cookie", createDeletionCookie("access_token", isSecure).toString());
        response.addHeader(
                "Set-Cookie", createDeletionCookie("refresh_token", isSecure).toString());
    }

    private ResponseCookie createAccessTokenCookie(String token) {
        return createAccessTokenCookie(token, sslEnabled);
    }

    private ResponseCookie createAccessTokenCookie(String token, boolean isSecure) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(jwtUtil.ACCESS_TOKEN_EXPIRATION / 1000)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String token) {
        return createRefreshTokenCookie(token, sslEnabled);
    }

    private ResponseCookie createRefreshTokenCookie(String token, boolean isSecure) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(jwtUtil.REFRESH_TOKEN_EXPIRATION / 1000)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie createDeletionCookie(String name, boolean isSecure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /**
     * Determine if cookies should have Secure flag based on request headers. In production, SSL
     * often terminates at load balancer/proxy, so X-Forwarded-Proto header indicates the actual
     * protocol used by the client.
     */
    private boolean determineSecureFlag(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }
        return sslEnabled || request.isSecure();
    }
}
