package com.univers.univers_backend.Controller;


import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Service.UserAuthenticationService;


@RestController
@RequestMapping ("/userauthentication")
public class UserController {
    @Autowired
    UserAuthenticationService userAuthenticationService;

    @GetMapping ("/print")
    public String toSting() {
        return "UserAuthenticationController";
    }

    @GetMapping ("/getAllUserAuthentication")
    public List<User> getAllUserAuthentication() {
        return userAuthenticationService.getAllUserAuthentication();
    }

    @PostMapping ("/register")
    public User addUserAuthentication(@RequestBody User userAuthentication) {
        return userAuthenticationService.register(userAuthentication);
    }   

    @PutMapping("/updateUserAuthentication")
    public User updateAuthentication(@RequestParam int sid, @RequestBody User newUserAuthentication) {
        return userAuthenticationService.updateAuthentication(sid, newUserAuthentication);
    }

    @DeleteMapping("/deleteUserAuthentication")
    public String deleteUserAuthentication(@RequestParam int sid) {
        return userAuthenticationService.deleteUserAuthentication(sid);
    }

    @GetMapping("/findByUsername")
    public User findByEmail(@RequestParam String email) {
        return userAuthenticationService.findByEmail(email);
    }



}