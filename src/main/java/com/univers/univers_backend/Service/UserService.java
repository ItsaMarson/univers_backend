package com.univers.univers_backend.Service;

import java.time.LocalDateTime;
import java.util.*;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Role;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final VenueRepository venueRepository;
    @Value("${mailjet.template.id.forgot.password}")
    private Long resetPassTemplateId;
    @Value("${mailjet.template.id}")
    private Long registerTemplateId;

    public UserService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository,
            DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder, EmailService emailService,
            VenueRepository venueRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.venueRepository = venueRepository;
    }

    public ResponseEntity<Map<String, Object>> login(LoginRequest request, HttpServletResponse response) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            String accessToken = jwtUtil.generateAccessToken(authentication.getName());
            String refreshToken = jwtUtil.generateRefreshToken(authentication.getName());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("User not found")); // This should never happen if
                                                                                // authentication passed

            // Store the access token in a cookie
            Cookie accessCookie = new Cookie("access_token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false); // Change to true in production
            accessCookie.setPath("/");
            accessCookie.setMaxAge(900000); // 15 minutes
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false); // Change to true in production
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800000);
            response.addCookie(refreshCookie);

            // Construct response payload
            Map<String, Object> responseBody = Map.of(
                    // "accessToken", accessToken,
                    // "refreshToken", refreshToken,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "first_name", user.getFirstname() != null ? user.getFirstname() : "",
                            "last_name", user.getLastname() != null ? user.getLastname() : "",
                            "roles", user.getRoles()));

            return ResponseEntity.ok(responseBody);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid email or password"));
        }

    }

    public String register(RegisterDTO request) {

        if (userRepository.existsByEmail(request.email())) {
            return "Email already in use";
        }
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Role.ORGANIZER);
        user.setFirstname(request.firstName() != null ? request.firstName() : "User");
        user.setLastname(request.lastName());
        user.setId_number(request.idNumber());
        user.setPhone_number(request.phoneNumber());
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId()).orElse(null);

            if (department == null) {
                return "Invalid department";
            }
            user.setDepartment(department);
        }

        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(10));
        user.setActive(true);
        userRepository.save(user);

        String subject = "Thanks for Signing Up. Please Verify Your Email Address [UniVERS] ";
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationCode, user.getFirstname(), registerTemplateId,
                subject);

        return "User registered successfully. Please check your email for the verification code.";
    }

    public String logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("access_token", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        Cookie refresh = new Cookie("refresh_token", "");
        refresh.setPath("/");
        refresh.setMaxAge(0);
        response.addCookie(refresh);

        return "Logged out successfully";
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getId_number(),
                        user.getPhone_number(),
                        user.getRoles().name(),
                        user.getDepartment().getId(),
                        user.getEmailVerified(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()))
                .toList();
    }

    public String forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String resetCode = String.format("%06d", new Random().nextInt(1000000));
        user.setVerificationCode(resetCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(15)); // Expire in 15 mins
        userRepository.save(user);

        Long templateId = resetPassTemplateId;
        String subject = "Reset Password [UniVERS]";
        emailService.sendVerificationEmail(user.getEmail(), resetCode, user.getFirstname(), templateId, subject);

        return "Password reset code sent successfully.";
    }

    public String resetPassword(String email, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "Password reset successfully";
    }

    public String createUser(CreateUserDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            return "Email already in use";
        }
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(request.role() != null ? request.role() : Role.ORGANIZER);
        user.setFirstname(request.firstName() != null ? request.firstName() : "User");
        user.setLastname(request.lastName());
        user.setId_number(request.idNumber());
        user.setPhone_number(request.phoneNumber());
        user.setDepartment(request.department());
        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(10));
        user.setActive(true);
        userRepository.save(user);

        String subject = "Thanks for Signing Up. Please Verify Your Email Address [UniVERS] ";
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationCode, user.getFirstname(), registerTemplateId,
                subject);

        return "User registered successfully. Please check your email for the verification code.";

    }

    public String updateUserProfile(Long userId, UserDTO updatedUser) {

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isEmpty()) {
            return "User does not exist";
        }

        User user = existingUser.get();
        user.setFirstname(updatedUser.firstName() != null ? updatedUser.firstName() : user.getFirstname());
        user.setLastname(updatedUser.lastName() != null ? updatedUser.lastName() : user.getLastname());
        user.setPhone_number(updatedUser.phoneNumber() != null ? updatedUser.phoneNumber() : user.getPhone_number());
        user.setId_number(updatedUser.idNumber() != null ? updatedUser.idNumber() : user.getId_number());
        if (updatedUser.department_id() != null) {
            Department myDept = departmentRepository.findById(updatedUser.department_id()).orElse(null);

            if (myDept == null) {
                return "Invalid department Id";
            }
            user.setDepartment(myDept);
        }
        userRepository.save(user);
        return "User details updated successfully.";
    }

    public String deactivateUser(Long userId) {
        Optional<User> existingUser = userRepository.findById(userId);

        if (existingUser.isEmpty()) {
            return "User not found";
        }
        User user = existingUser.get();
        user.setActive(false);
        userRepository.save(user);
        return "User deactivated successfully";
    }

    public String activateUser(Long userId) {
        Optional<User> existingUser = userRepository.findById(userId);

        if (existingUser.isEmpty()) {
            return "User not found";
        }
        User user = existingUser.get();
        user.setActive(true);
        userRepository.save(user);
        return "User activated successfully";
    }

    public UserDTO getCurrentUser(String token) {
        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getRoles().name(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public String verifyResetCode(String email, String verificationCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCode() == null ||
                !user.getVerificationCode().equals(verificationCode)) {
            return "Invalid reset code";
        }

        if (user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
            return "Reset code has expired. Please request a new one.";
        }

        return "Valid code"; // Indicate that the code is valid
    }

    public String resetPassword(String email, String verificationCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCode() == null ||
                !user.getVerificationCode().equals(verificationCode)) {
            return "Invalid reset code";
        }

        if (user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
            return "Reset code has expired";
        }

        if (newPassword == null || newPassword.isEmpty()) {
            return "New password cannot be empty";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationCode(null);
        user.setVerificationCodeExpiration(null);
        userRepository.save(user);

        return "Password reset successfully";
    }

    public VenueDTO getManagedVenue(Long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            throw new RuntimeException("User not found with ID " + userId);
        }
        User venueOwner = user.get();
        Optional<Venue> venueOptional = venueRepository.findByVenueOwner(venueOwner);

        if (venueOptional.isEmpty()) {
            throw new RuntimeException("No venue managed by this user");
        }

        Venue venue = venueOptional.get();

        return new VenueDTO(
                venue.getId(),
                venue.getName(),
                venue.getLocation(),
                null,
                null,
                null);

    }

}
