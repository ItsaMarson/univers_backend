package com.univers.univers_backend.Service;

import java.util.*;

import com.univers.univers_backend.DTO.RegisterRequest;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private  final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            return "Email already in use";
        }
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles() != null ? request.roles() : Role.ORGANIZER);
        user.setEmailVerified(false);
        user.setVerificationCode(verificationCode);
        userRepository.save(user);

        //Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return "User registered successfully. Please check your email for the verification code.";
    }

    public List<UserDTO>getAllUsers(){
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserDTO(
                        user.getEmail(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getId_number(),
                        user.getPhone_number(),
                        user.getRoles().name(),
                        user.getEmailVerified()
                )).toList();
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