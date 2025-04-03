package com.univers.univers_backend.Controller;



import com.univers.univers_backend.DTO.CreateUserDTO;
import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.Service.DepartmentService;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.Service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {


    private final UserService userService;
    private final DepartmentService departmentService;

    private final VenueService venueService;

    public AdminController(UserService userService, DepartmentService departmentService, VenueService venueService){
        this.userService = userService;
        this.departmentService = departmentService;
        this.venueService = venueService;
    }
    @PostMapping("/users")
    public ResponseEntity<String> createUser(@Valid @RequestBody CreateUserDTO request){
        String responseMessage = userService.createUser(request);
        if("Email already in use".equals(responseMessage)){
            ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers(){
        List<UserDTO> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }
    @PostMapping("/departments")
    public ResponseEntity<String> addDepartment(@RequestBody DepartmentDTO departmentDTO){
        String responseMessage = departmentService.addDepartment(departmentDTO);

        if("Department already exists.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok().body(responseMessage);
    }

    @PatchMapping("/departments/{departmentId}")
    public ResponseEntity<String> updateDepartment(@PathVariable Long departmentId, @RequestBody DepartmentDTO updatedDept){
        String responseMessage = departmentService.updateDepartment(departmentId, updatedDept);

        if("Department does not exist".equals(responseMessage) || "Invalid department head".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/{departmentId}/assign-head/{userId}")
    public ResponseEntity<String> assignDepartmentHead(@PathVariable Long departmentId, @PathVariable Long userId) {
        String message = departmentService.assignDepartmentHead(departmentId, userId);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/users/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long userId) {
        String responseMessage = userService.deactivateUser(userId);
        if("User not found".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long userId){
        String responseMessage = userService.deactivateUser(userId);
        if("User not found".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/venues")
    public ResponseEntity<String> addVenue(@RequestBody VenueDTO venueDTO){
        String responseMessage = venueService.addVenue(venueDTO);

        if("Venue already exists.".equals(responseMessage) || "User not found.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

}
