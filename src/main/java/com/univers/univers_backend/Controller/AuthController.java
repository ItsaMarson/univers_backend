package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.LoginRequest;
import com.univers.univers_backend.DTO.RegisterRequest;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Service.EmailService;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    private final EmailService emailService;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil, UserService userService, UserRepository userRepository, EmailService emailService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String responseMessage = userService.register(request);
        if("Email already in use".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String accessToken = jwtUtil.generateAccessToken(authentication.getName());
        String refreshToken = jwtUtil.generateRefreshToken(authentication.getName());

        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new RuntimeException("User not found"));
        // Store the access token in a cookie
        Cookie cookie = new Cookie("jwt", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); //Change to true in production
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60); // 15 minutes
        response.addCookie(cookie);

        // Create response payload
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "first_name", user.getFirstname() != null ? user.getFirstname() : "",
                "last_name", user.getLastname() != null ? user.getLastname() : "",
                "roles", user.getRoles()
        ));

        return ResponseEntity.ok(responseBody);
    }
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }

        String email = jwtUtil.extractUsername(refreshToken);
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }
    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("verification_code");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if(user.getVerificationCode() == null || !user.getVerificationCode().equals(code)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid verification code.");
        }

        if(user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Verification code has expired. Please request a new one");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiration(null);
        userRepository.save(user);

        return ResponseEntity.ok("Email verified successfully!");

    }
    @PostMapping("/resend-code")
    public ResponseEntity<String> resendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        String responseMessage = emailService.resendVerificationCode(email);
        if("User not found.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        if("Email is already verified.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok("A new verification code has been sent to your email.");
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        String responseMessage = userService.logout(response);
        return ResponseEntity.ok(responseMessage);
    }

}
