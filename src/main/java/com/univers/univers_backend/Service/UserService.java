package com.univers.univers_backend.Service;

import java.time.LocalDateTime;
import java.util.*;

import com.univers.univers_backend.DTO.LoginRequest;
import com.univers.univers_backend.DTO.RegisterDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Role;
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

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;
    private  final EmailService emailService;

    @Value("${mailjet.template.id.forgot.password}")
    private Long resetPassTemplateId;
    @Value("${mailjet.template.id}")
    private Long registerTemplateId;

    public UserService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService){
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public ResponseEntity<Map<String, Object>> login(LoginRequest request, HttpServletResponse response) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            String accessToken = jwtUtil.generateAccessToken(authentication.getName());
            String refreshToken = jwtUtil.generateRefreshToken(authentication.getName());

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("User not found")); // This should never happen if authentication passed

            // Store the access token in a cookie
            Cookie cookie = new Cookie("jwt", accessToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Change to true in production
            cookie.setPath("/");
            cookie.setMaxAge(15 * 60); // 15 minutes
            response.addCookie(cookie);

            // Construct response payload
            Map<String, Object> responseBody = Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "first_name", user.getFirstname() != null ? user.getFirstname() : "",
                            "last_name", user.getLastname() != null ? user.getLastname() : "",
                            "roles", user.getRoles()
                    )
            );

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
        user.setRoles(request.roles() != null ? request.roles() : Role.ORGANIZER);
        user.setFirstname(request.firstName() != null ? request.firstName() : "User");
        user.setLastname(request.lastName());
        user.setId_number(request.idNumber());
        user.setPhone_number(request.phoneNumber());
        user.setDepartment(request.department());
        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        Long templateId = registerTemplateId;
        String subject = "Thanks for Signing Up. Please Verify Your Email Address [UniVERS] ";
        //Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationCode, user.getFirstname(), templateId, subject);

        return "User registered successfully. Please check your email for the verification code.";
    }

    public String logout(HttpServletResponse response){

        Cookie cookie = new Cookie("jwt", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "Logged out successfully";
    }

    public List<UserDTO>getAllUsers(){
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
                        user.getEmailVerified()
                )).toList();
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



//    public User findByEmail(String email) {
//        return userRepository.findByEmail(email);
//    }

//public User updateAuthentication(int sid, User newuser) {
//
//    try {
//        User user = userRepository.findById(sid).get();
//        user.setEmail(newuser.getEmail());
//        user.setPassword(newuser.getPassword());
//        user.setRole(newuser.getRole());
//    } catch (NoSuchElementException e) {
//        throw new NoSuchElementException("No such user exists" + sid);
//    } finally {
//        return userRepository.save(newuser);
//    }
//}
//
//public String deleteUserAuthentication(int sid) {
//    try {
//        userRepository.deleteById(sid);
//    } catch (NoSuchElementException e) {
//        throw new NoSuchElementException("No such user exists" + sid);
//    } finally {
//        return "User Deleted";
//    }
//}


}