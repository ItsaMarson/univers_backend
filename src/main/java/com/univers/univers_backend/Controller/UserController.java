/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<String> updateUserProfile(
            @PathVariable Long userId,
            @RequestPart("userDTO") UserDTO userDTO,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        String responseMessage = userService.updateUserProfile(userId, userDTO, imageFile);
        if ("User does not exist".equals(responseMessage)
                || "Invalid department Id".equals(responseMessage)) {
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @GetMapping("/{userId}/managed-venue")
    public ResponseEntity<VenueDTO> getManagedVenue(@PathVariable Long userId) {
        VenueDTO managedVenue = userService.getManagedVenue(userId);

        return ResponseEntity.ok(managedVenue);
    }


}
