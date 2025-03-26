package com.univers.univers_backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final String SECRET_KEY = "02d07dfeee83f54d3f9624178a7c0ba54e8b33eca43ce9a049188f83218613a81263dc344ef7dcc89e7aaf35ad597d6c9c9cc9ccee0cbf2f1024a0c71dc846f9a7d7537fd010da8928f9c7bdc6d36436cb5c8091fd5f078e18498d5793f2a2d11252f1d238584b3a6e098fe075e7653d115a0535cacd9fd4e5989c931558de6f3efbcbbcce29fc86eefab807735d6e9e096505323269da855c33b7d6e08acbfa5321ec16e446588243baced8e181b79016a20187ae69b20402d677642edb36a4fecb9bd3fcdd0113f473ce7cc4abc7418c51de119cf87b9f92b9cbcb9cce27ee399a1fe101f659dd3352aa3e22190c396429c18dacef62ae396cfd0b0ac703ff";
    private final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
