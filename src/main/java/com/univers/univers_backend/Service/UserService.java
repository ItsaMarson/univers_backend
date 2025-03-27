package com.univers.univers_backend.Service;

import java.util.*;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Repository.UserRepository;


@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public String register(UserDTO userDTO) {

        if (userRepository.existsByEmail(userDTO.email())) {
            return "Email already in use";
        }

        User user = new User();
        user.setEmail(userDTO.email());
        user.setPassword(passwordEncoder.encode(userDTO.password()));
        user.setRoles(Set.of(Role.ORGANIZER));//default role
        userRepository.save(user);

        return "User registered successfully ";
    }

//public List<User> getAllUserAuthentication() {
//    return userRepository.findAll();
//}
//
//public User findByEmail(String email) {
//    return userRepository.findByEmail(email);
//}
//
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