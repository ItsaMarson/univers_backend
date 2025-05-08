/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.CreateUserDTO;
import com.univers.univers_backend.DTO.EditUserDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.LoginRequest;
import com.univers.univers_backend.DTO.RegisterDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Mapper.VenueMapper;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.config.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final VenueRepository venueRepository;

    private final EventRepository eventRepository;
    private final EventService eventService;

    private final FileStorageService fileStorageService;

    private final UserMapper userMapper;
    private final VenueMapper venueMapper;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    @Value("${mailjet.template.id.forgot.password}")
    private Long resetPassTemplateId;

    @Value("${mailjet.template.id}")
    private Long registerTemplateId;

    public UserService(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            VenueRepository venueRepository,
            FileStorageService fileStorageService,
            EventRepository eventRepository,
            EventService eventService,
            UserMapper userMapper,
            VenueMapper venueMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.venueRepository = venueRepository;
        this.fileStorageService = fileStorageService;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
    }

    public ResponseEntity<Map<String, Object>> login(
            LoginRequest request, HttpServletResponse response) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.email(), request.password()));

            User user =
                    userRepository
                            .findByEmail(request.email())
                            .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(
                                Map.of(
                                        "error",
                                        "Email not verified. Please verify your email before"
                                                + " logging in."));
            }

            String accessToken = jwtUtil.generateAccessToken(authentication.getName());
            String refreshToken = jwtUtil.generateRefreshToken(authentication.getName());

            Cookie accessCookie = new Cookie("access_token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(604800000);
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(604800000);
            response.addCookie(refreshCookie);

            UserDTO userDto = userMapper.toDto(user);

            Map<String, Object> responseBody = Map.of("user", userDto);

            return ResponseEntity.ok(responseBody);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (InternalAuthenticationServiceException e) {
            if (e.getCause() instanceof UsernameNotFoundException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid email or password"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "error",
                                    "An internal server error occurred during login: "
                                            + e.getMessage()));
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
        user.setTelephoneNumber(request.telephoneNumber());

        if (request.departmentPublicId() != null) {
            Department department =
                    departmentRepository.findByPublicId(request.departmentPublicId()).orElse(null);
            if (department == null) {
                return "Invalid department public ID";
            }
            user.setDepartment(department);
        }

        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(10));
        user.setActive(true);
        userRepository.save(user);

        String subject = "Thanks for Signing Up. Please Verify Your Email Address [UniVERS] ";
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationCode,
                user.getFirstname(),
                registerTemplateId,
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
        return users.stream().map(userMapper::toDto).toList();
    }

    public String forgotPassword(String email) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found with email: " + email));
        String resetCode = String.format("%06d", new Random().nextInt(1000000));
        user.setVerificationCode(resetCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String subject = "Password Reset Code [UniVERS]";
        try {
            emailService.sendVerificationEmail(
                    user.getEmail(), resetCode, user.getFirstname(), resetPassTemplateId, subject);
        } catch (Exception e) {
            System.err.println(
                    "Error sending password reset email for user " + email + ": " + e.getMessage());
        }

        return "Password reset code sent to your email.";
    }

    public String resetPassword(String email, String newPassword) {

        User user =
                userRepository
                        .findByEmail(email)
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
        user.setTelephoneNumber(request.telephoneNumber());

        if (request.departmentPublicId() != null) {
            Department department =
                    departmentRepository.findByPublicId(request.departmentPublicId()).orElse(null);
            if (department == null) {
                return "Invalid department public ID";
            }
            user.setDepartment(department);
        }

        user.setEmailVerified(request.emailVerified() != null ? request.emailVerified() : false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(10));
        user.setActive(true);
        userRepository.save(user);

        String subject = "Thanks for Signing Up. Please Verify Your Email Address [UniVERS] ";
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationCode,
                user.getFirstname(),
                registerTemplateId,
                subject);

        return "User created successfully with email: " + user.getEmail();
    }

    @Transactional
    public String updateUserProfile(
            UUID publicId, EditUserDTO updatedUserDto, MultipartFile imageFile) {
        User user =
                userRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with public ID: " + publicId));

        if (updatedUserDto.firstName() != null) user.setFirstname(updatedUserDto.firstName());
        if (updatedUserDto.lastName() != null) user.setLastname(updatedUserDto.lastName());
        if (updatedUserDto.idNumber() != null) user.setId_number(updatedUserDto.idNumber());
        if (updatedUserDto.phoneNumber() != null)
            user.setPhone_number(updatedUserDto.phoneNumber());
        if (updatedUserDto.telephoneNumber() != null)
            user.setTelephoneNumber(updatedUserDto.telephoneNumber());

        if (updatedUserDto.departmentPublicId() != null) {
            Department department =
                    departmentRepository
                            .findByPublicId(updatedUserDto.departmentPublicId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Department not found with public ID: "
                                                            + updatedUserDto.departmentPublicId()));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
                try {
                    fileStorageService.deleteFile(user.getProfileImagePath(), usersBucketName);
                } catch (Exception e) {
                    System.err.println("Error deleting old profile image: " + e.getMessage());
                }
            }
            try {
                String imagePath =
                        fileStorageService.uploadFile(
                                imageFile, usersBucketName, "user-profile-images/");
                user.setProfileImagePath(imagePath);
            } catch (Exception e) {
                System.err.println("Error saving new profile image: " + e.getMessage());
                throw new RuntimeException("Error updating profile image.", e);
            }
        }
        userRepository.save(user);
        return "User profile updated successfully.";
    }

    @Transactional
    public String editUserAsAdmin(UUID publicId, EditUserDTO editUserDTO, MultipartFile imageFile) {
        User user =
                userRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with public ID: " + publicId));

        if (editUserDTO.email() != null && !editUserDTO.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(editUserDTO.email())) {
                throw new RuntimeException("Error: Email already in use by another account.");
            }
            user.setEmail(editUserDTO.email());
        }

        if (editUserDTO.firstName() != null) user.setFirstname(editUserDTO.firstName());
        if (editUserDTO.lastName() != null) user.setLastname(editUserDTO.lastName());
        if (editUserDTO.idNumber() != null) user.setId_number(editUserDTO.idNumber());
        if (editUserDTO.phoneNumber() != null) user.setPhone_number(editUserDTO.phoneNumber());
        if (editUserDTO.telephoneNumber() != null)
            user.setTelephoneNumber(editUserDTO.telephoneNumber());

        if (editUserDTO.departmentPublicId() != null) {
            Department department =
                    departmentRepository
                            .findByPublicId(editUserDTO.departmentPublicId())
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Department not found with Public ID: "
                                                            + editUserDTO.departmentPublicId()));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        if (editUserDTO.role() != null) {
            try {
                user.setRoles(Role.valueOf(editUserDTO.role().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Error: Invalid role specified.", e);
            }
        }
        if (editUserDTO.active() != null) user.setActive(editUserDTO.active());
        if (editUserDTO.emailVerified() != null) user.setEmailVerified(editUserDTO.emailVerified());

        if (imageFile != null && !imageFile.isEmpty()) {
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
                try {
                    fileStorageService.deleteFile(user.getProfileImagePath(), usersBucketName);
                } catch (Exception e) {
                    System.err.println("Error deleting old profile image: " + e.getMessage());
                }
            }
            try {
                String imagePath =
                        fileStorageService.uploadFile(
                                imageFile, usersBucketName, "user-profile-images/");
                user.setProfileImagePath(imagePath);
            } catch (Exception e) {
                System.err.println("Error saving new profile image: " + e.getMessage());
                throw new RuntimeException("Error: Could not update profile image.", e);
            }
        }
        userRepository.save(user);
        return "User details updated successfully by admin.";
    }

    public String deactivateUser(UUID publicId) {
        User user =
                userRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with Public ID: " + publicId));
        user.setActive(false);
        userRepository.save(user);
        return "User deactivated successfully.";
    }

    public String activateUser(UUID publicId) {
        User user =
                userRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with Public ID: " + publicId));
        user.setActive(true);
        userRepository.save(user);
        return "User activated successfully.";
    }

    public UserDTO getCurrentUser(String token) {
        String username = jwtUtil.extractUsername(token);
        User user =
                userRepository
                        .findByEmail(username)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found with email: " + username));
        return userMapper.toDto(user);
    }

    public String verifyResetCode(String email, String verificationCode) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(verificationCode)) {
            return "Invalid reset code";
        }

        if (user.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
            return "Reset code has expired. Please request a new one.";
        }

        return "Valid code"; // Indicate that the code is valid
    }

    public String resetPassword(String email, String verificationCode, String newPassword) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(verificationCode)) {
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

    public VenueDTO getManagedVenue(UUID userPublicId) {
        User user =
                userRepository
                        .findByPublicId(userPublicId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "User not found with Public ID: " + userPublicId));

        Venue venue =
                venueRepository
                        .findByVenueOwner(user)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "No venue managed by user: " + user.getEmail()));

        return venueMapper.toDto(venue);
    }

    public List<EventDTO> getOwnEvents() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser =
                userRepository
                        .findByEmail(currentUsername)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User not found: " + currentUsername));

        List<Event> events = eventRepository.findByOrganizer(currentUser);
        return events.stream().map(eventService::mapToDTO).collect(Collectors.toList());
    }
}
