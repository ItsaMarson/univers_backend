/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.CreateUserDTO;
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
import java.util.Optional;
import java.util.Random;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

    private final EventRepository eventRepository; // Inject EventRepository
    private final EventService eventService; // Inject EventService (for mapping)

    private final FileStorageService fileStorageService;

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
            EventService eventService) {
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

            Map<String, Object> responseBody =
                    Map.of(
                            // "accessToken", accessToken,
                            // "refreshToken", refreshToken,
                            "user",
                            Map.of(
                                    "id",
                                    user.getId(),
                                    "email",
                                    user.getEmail(),
                                    "first_name",
                                    user.getFirstname() != null ? user.getFirstname() : "",
                                    "last_name",
                                    user.getLastname() != null ? user.getLastname() : "",
                                    "roles",
                                    user.getRoles()));

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
                    .body(Map.of("error", "An internal server error occurred during login."));
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
        user.setTelephoneNumber(request.telephoneNumber());
        if (request.departmentId() != null) {
            Department department =
                    departmentRepository.findById(request.departmentId()).orElse(null);

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
        return users.stream()
                .map(this::mapUserToDTO) // Use the helper method
                .toList();
    }

    public String forgotPassword(String email) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        String resetCode = String.format("%06d", new Random().nextInt(1000000));
        user.setVerificationCode(resetCode);
        user.setVerificationCodeExpiration(
                LocalDateTime.now().plusMinutes(15)); // Expire in 15 mins
        userRepository.save(user);

        Long templateId = resetPassTemplateId;
        String subject = "Reset Password [UniVERS]";
        emailService.sendVerificationEmail(
                user.getEmail(), resetCode, user.getFirstname(), templateId, subject);

        return "Password reset code sent successfully.";
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
        if (request.departmentId() != null) {
            Department department =
                    departmentRepository.findById(request.departmentId()).orElse(null);

            if (department == null) {
                return "Department not found. Invalid department Id";
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
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationCode,
                user.getFirstname(),
                registerTemplateId,
                subject);

        return "User registered successfully. Please check your email for the verification code.";
    }

    public String updateUserProfile(Long userId, UserDTO updatedUser, MultipartFile imageFile) {

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isEmpty()) {
            return "User does not exist";
        }

        User user = existingUser.get();
        user.setFirstname(
                updatedUser.firstName() != null ? updatedUser.firstName() : user.getFirstname());
        user.setLastname(
                updatedUser.lastName() != null ? updatedUser.lastName() : user.getLastname());
        user.setPhone_number(
                updatedUser.phoneNumber() != null
                        ? updatedUser.phoneNumber()
                        : user.getPhone_number());
        user.setTelephoneNumber(
                updatedUser.telephoneNumber() != null
                        ? updatedUser.telephoneNumber()
                        : user.getTelephoneNumber());
        user.setId_number(
                updatedUser.idNumber() != null ? updatedUser.idNumber() : user.getId_number());
        if (updatedUser.departmentId() != null) {
            Department myDept =
                    departmentRepository.findById(updatedUser.departmentId()).orElse(null);

            if (myDept == null) {
                return "Invalid department Id";
            }
            user.setDepartment(myDept);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
                fileStorageService.deleteFile(user.getProfileImagePath(), usersBucketName);
            }
            String newObjectName =
                    fileStorageService.uploadFile(
                            imageFile, usersBucketName, "user-profile-images/");
            user.setProfileImagePath(newObjectName);
        }
        userRepository.save(user);
        return "User details updated successfully.";
    }

    public String editUserAsAdmin(Long userId, UserDTO updatedUser, MultipartFile imageFile) {

        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isEmpty()) {
            return "User does not exist";
        }

        User user = existingUser.get();
        user.setFirstname(
                updatedUser.firstName() != null ? updatedUser.firstName() : user.getFirstname());
        user.setLastname(
                updatedUser.lastName() != null ? updatedUser.lastName() : user.getLastname());
        user.setRoles(
                updatedUser.role() != null ? Role.valueOf(updatedUser.role()) : user.getRoles());
        user.setPhone_number(
                updatedUser.phoneNumber() != null
                        ? updatedUser.phoneNumber()
                        : user.getPhone_number());
        user.setTelephoneNumber(
                updatedUser.telephoneNumber() != null
                        ? updatedUser.telephoneNumber()
                        : user.getTelephoneNumber());
        user.setId_number(
                updatedUser.idNumber() != null ? updatedUser.idNumber() : user.getId_number());

        if (updatedUser.departmentId() != null) {
            if (user.getDepartment() == null
                    || !updatedUser.departmentId().equals(user.getDepartment().getId())) {
                Department myDept =
                        departmentRepository.findById(updatedUser.departmentId()).orElse(null);

                if (myDept == null) {
                    return "Invalid department Id";
                }
                user.setDepartment(myDept);
            }
        } else {
            user.setDepartment(null);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
                    fileStorageService.deleteFile(user.getProfileImagePath(), usersBucketName);
                }
                String newObjectName =
                        fileStorageService.uploadFile(
                                imageFile, usersBucketName, "user-profile-images/");
                user.setProfileImagePath(newObjectName);
            } catch (Exception e) {
                System.err.println(
                        "Failed to update profile image for user "
                                + userId
                                + ": "
                                + e.getMessage());
                return "Failed to update profile image due to a storage error.";
            }
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
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        return mapUserToDTO(user);
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
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }

    public List<EventDTO> getOwnEvents() {
        String currentEmail =
                ((UserDetails)
                                SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal())
                        .getUsername();
        User currentUser =
                userRepository
                        .findByEmail(currentEmail)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        List<Event> events = eventRepository.findByOrganizer(currentUser);

        return events.stream().map(eventService::mapToDTO).collect(Collectors.toList());
    }

    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
            try {
                profileImageUrl =
                        fileStorageService.getFileUrl(user.getProfileImagePath(), usersBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for user "
                                + user.getId()
                                + ": "
                                + e.getMessage());
            }
        }
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname() != null ? user.getFirstname() : null,
                user.getLastname() != null ? user.getLastname() : null,
                user.getId_number() != null ? user.getId_number() : null,
                user.getPhone_number() != null ? user.getPhone_number() : null,
                user.getTelephoneNumber() != null ? user.getTelephoneNumber() : null,
                user.getRoles() != null ? user.getRoles().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
