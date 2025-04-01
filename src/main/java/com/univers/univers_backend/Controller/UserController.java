package com.univers.univers_backend.Controller;


import java.util.*;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping ("/users")
public class UserController {


    private final UserService userService;
    public UserController(UserService userService){
        this.userService = userService;
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> request){
        String email = request.get("email");
        String responseMessge = userService.forgotPassword(email);

        return ResponseEntity.ok(responseMessge);
    }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request){
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        String responseMessage= userService.resetPassword(email, newPassword);

        if("Invalid reset code".equals(responseMessage) || "Reset code has expired. Please request a new one.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }

        return ResponseEntity.ok(responseMessage);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable Long userId, @RequestBody UserDTO userDTO){

        String responseMessage = userService.updateUserProfile(userId, userDTO);
        if("User does not exist".equals(responseMessage) || "Invalid department Id".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

}