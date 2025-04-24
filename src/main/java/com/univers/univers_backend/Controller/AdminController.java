package com.univers.univers_backend.Controller;



import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.univers.univers_backend.DTO.CreateUserDTO;
import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Service.DepartmentService;
import com.univers.univers_backend.Service.UserService;
import com.univers.univers_backend.Service.VenueService;

import jakarta.validation.Valid;

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
    public ResponseEntity<?> addVenue(@RequestPart("venue") VenueDTO venueDTO,
                                      @RequestPart(value = "image", required = false) MultipartFile imageFile){

        try{
            VenueDTO newVenue = venueService.addVenue(venueDTO, imageFile);
            return new ResponseEntity<>(newVenue, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PatchMapping("/venues/{venueId}")
    public ResponseEntity<?> updateVenue(@PathVariable Long venueId,
                                         @RequestPart("venue") VenueDTO venueDTO,
                                         @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            VenueDTO updatedVenue = venueService.updateVenue(venueId, venueDTO, imageFile);
            return ResponseEntity.ok(updatedVenue);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { // Catch potential file saving/deletion errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during venue update: " + e.getMessage());
        }
    }

    @DeleteMapping("/venues/{venueId}")
    public ResponseEntity<?> deleteVenue(@PathVariable Long venueId) {
        try {
            venueService.deleteVenue(venueId);
            return ResponseEntity.ok("Venue with ID " + venueId + " deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Could not delete venue. It might be associated with existing events or an error occurred during image deletion.");
            // Or a more generic internal server error:
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the venue.");
        }
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<String> editUserAsAdmin(
            @PathVariable Long userId, @RequestBody UserDTO userDTO) {

        String responseMessage = userService.editUserAsAdmin(userId, userDTO);
        if ("User does not exist".equals(responseMessage)
                || "Invalid department Id".equals(responseMessage)) {
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

}
