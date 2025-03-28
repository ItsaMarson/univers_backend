package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.RegisterRequest;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Service.DepartmentService;
import com.univers.univers_backend.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {


    private final UserService userService;
    private final DepartmentService departmentService;

    public AdminController(UserService userService, DepartmentService departmentService){
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(){
        List<UserDTO> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }
    @PostMapping("/{departmentId}/assign-head/{userId}")
    public ResponseEntity<String> assignDepartmentHead(@PathVariable Long departmentId, @PathVariable Long userId) {
        String message = departmentService.assignDepartmentHead(departmentId, userId);
        return ResponseEntity.ok(message);
    }

//    @DeleteMapping("/users/{userId}/deactivate")
//    public ResponseEntity<String> deactivateUser(@PathVariable Long userId) {
//
//        return ResponseEntity.ok(responseMessage);
//    }
//
//    @PostMapping("/users/{userId}/activate")
//    public ResponseEntity<String> activateUser(@PathVariable Long userId){
//
//    }

}
