//package com.univers.univers_backend.Service;
//
//import java.util.*;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.univers.univers_backend.Entity.User;
//import com.univers.univers_backend.Repository.UserRepository;
//
//
//    @Service
//    public class UserService {
//
//    @Autowired
//    private UserRepository userRepository;
//
//
//    public User register(User registeruser) {
//        return userRepository.save(registeruser);
//    }
//
//    public List<User> getAllUserAuthentication() {
//        return userRepository.findAll();
//    }
//
//    public User findByEmail(String email) {
//        return userRepository.findByEmail(email);
//    }
//
//    public User updateAuthentication(int sid, User newuser) {
//
//        try {
//            User user = userRepository.findById(sid).get();
//            user.setEmail(newuser.getEmail());
//            user.setPassword(newuser.getPassword());
//            user.setRole(newuser.getRole());
//        } catch (NoSuchElementException e) {
//            throw new NoSuchElementException("No such user exists" + sid);
//        } finally {
//            return userRepository.save(newuser);
//        }
//    }
//
//    public String deleteUserAuthentication(int sid) {
//        try {
//            userRepository.deleteById(sid);
//        } catch (NoSuchElementException e) {
//            throw new NoSuchElementException("No such user exists" + sid);
//        } finally {
//            return "User Deleted";
//        }
//    }
//
//
//}