/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.LoginRequest;
import com.univers.univers_backend.DTO.RegisterDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Service.EmailService;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.config.ApiResponse;
import com.univers.univers_backend.config.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    private final EmailService emailService;

    List<String> duplicateMessages = List.of(
            "Email already in use",
            "Id number already in use"
    );
    public AuthController(
            AuthenticationManager authManager,
            JwtUtil jwtUtil,
            UserService userService,
            UserRepository userRepository,
            EmailService emailService,
            UserDetailsService userDetailsService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userDetailsService = userDetailsService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided details")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User registered successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Email already in use")
            })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterDTO request) {
        String responseMessage = userService.register(request);
        if (duplicateMessages.contains(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(
                            ApiResponse.error(
                                    HttpStatus.BAD_REQUEST.value(),
                                    "Registration failed",
                                    responseMessage));
        }
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully", responseMessage));
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns JWT tokens")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Login successful"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Invalid credentials")
            })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(), loginRequest.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        com.univers.univers_backend.Entity.User userDetails;
        try {
            UserDetails springUserDetails =
                    userDetailsService.loadUserByUsername(loginRequest.email());
            if (!(springUserDetails instanceof com.univers.univers_backend.Entity.User)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "User details configuration error"));
            }
            userDetails = (com.univers.univers_backend.Entity.User) springUserDetails;
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving user details after authentication"));
        }

        if (!userDetails.getEmailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(
                            Map.of(
                                    "error",
                                    "Email not verified. Please verify your email before logging"
                                            + " in."));
        }

        final String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

        Cookie accessTokenCookie = new Cookie("access_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (jwtUtil.ACCESS_TOKEN_EXPIRATION / 1000));

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (jwtUtil.REFRESH_TOKEN_EXPIRATION / 1000));

        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates new access and refresh tokens using a valid refresh token")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Tokens refreshed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Invalid refresh token")
            })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        Optional<Cookie> refreshTokenCookieOpt = Optional.empty();
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            refreshTokenCookieOpt =
                    Arrays.stream(cookies)
                            .filter(cookie -> "refresh_token".equals(cookie.getName()))
                            .findFirst();
        }

        if (refreshTokenCookieOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found.");
        }

        String refreshToken = refreshTokenCookieOpt.get().getValue();
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token.");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateAccessToken(userDetails.getUsername());
        Cookie newAccessTokenCookie = new Cookie("access_token", newAccessToken);
        newAccessTokenCookie.setHttpOnly(true);
        newAccessTokenCookie.setPath("/");
        newAccessTokenCookie.setMaxAge((int) (jwtUtil.ACCESS_TOKEN_EXPIRATION / 1000));
        response.addCookie(newAccessTokenCookie);

        return ResponseEntity.ok().body("Access token refreshed successfully.");
    }

    @Operation(
            summary = "Verify email",
            description = "Verifies user's email using the verification code")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Email verified successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Invalid or expired verification code"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "Email does not exist")
            })
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("verification_code");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Email does not exist"));
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.UNAUTHORIZED.value(), "Invalid verification code"));
        }

        if (user.getVerificationCodeExpiration().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            ApiResponse.error(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "Verification code has expired. Please request a new one"));
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiration(null);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @Operation(
            summary = "Resend verification code",
            description = "Sends a new verification code to the user's email")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "New verification code sent"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "User not found or email already verified")
            })
    @PostMapping("/resend-code")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String responseMessage = emailService.resendVerificationCode(email);

        if ("User not found.".equals(responseMessage)
                || "Email is already verified.".equals(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), responseMessage));
        }

        return ResponseEntity.ok(
                ApiResponse.success("A new verification code has been sent to your email"));
    }

    @Operation(
            summary = "User logout",
            description = "Logs out the current user by clearing authentication tokens")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        String responseMessage = userService.logout(response);
        return ResponseEntity.ok(ApiResponse.success(responseMessage));
    }

    @Operation(
            summary = "Get current user",
            description = "Retrieves the profile of the currently authenticated user")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "User profile retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated")
            })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie) {
        UserDTO user = userService.getCurrentUser();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("user", user);
        responseData.put("refreshToken", refreshTokenFromCookie);

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @Operation(
            summary = "Request password reset",
            description = "Initiates the password reset process by sending a verification code")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Reset code sent successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "User not found")
            })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String responseMessage = userService.forgotPassword(email);

        if ("User not found".equals(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), responseMessage));
        }

        return ResponseEntity.ok(ApiResponse.success(responseMessage));
    }

    @Operation(
            summary = "Verify reset code",
            description = "Validates the password reset verification code")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Valid code"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid code")
            })
    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse<String>> verifyResetCode(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String verificationCode = request.get("verificationCode");
        String responseMessage = userService.verifyResetCode(email, verificationCode);

        if ("Valid code".equals(responseMessage)) {
            return ResponseEntity.ok(ApiResponse.success(responseMessage));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), responseMessage));
        }
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using the verification code")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Password reset successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid code, expired code, or invalid request")
            })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String verificationCode = request.get("verificationCode");
        String newPassword = request.get("newPassword");

        String responseMessage = userService.resetPassword(email, verificationCode, newPassword);

        if ("Invalid verification code".equals(responseMessage)
                || "Verification code has expired".equals(responseMessage)
                || "User not found".equals(responseMessage)
                || "New password cannot be empty".equals(responseMessage)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), responseMessage));
        }

        return ResponseEntity.ok(ApiResponse.success(responseMessage));
    }
}
