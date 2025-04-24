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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.Service.EquipmentService;

@RestController
@RequestMapping("/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService){
        this.equipmentService = equipmentService;
    }

    @PostMapping
    public ResponseEntity<?> addEquipment(@RequestParam Long userId, 
                                          @RequestPart("equipment") EquipmentDTO equipmentDTO,
                                          @RequestPart(value = "image", required = false) MultipartFile imageFile){ 

        try{
            EquipmentDTO newEquipment = equipmentService.addEquipment(userId, equipmentDTO, imageFile);
            return  new ResponseEntity<>(newEquipment, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) { 
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/all") 
    public ResponseEntity<List<EquipmentDTO>> getAllEquipments() {
        List<EquipmentDTO> allEquipments = equipmentService.getAllEquipments();
        if (allEquipments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(allEquipments);
    }

    @GetMapping 
    public ResponseEntity<?> getAllEquipmentsByOwner(@RequestParam Long userId){
        try{
            List<EquipmentDTO> allEquipmentsByOwner = equipmentService.getAllEquipmentsByOwner(userId);
             if (allEquipmentsByOwner.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(allEquipmentsByOwner);
        }catch (IllegalArgumentException e){ 
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{equipmentId}")
    public ResponseEntity<?> getEquipmentById(@PathVariable Long equipmentId) {
        try {
            EquipmentDTO equipment = equipmentService.getEquipmentById(equipmentId);
            return ResponseEntity.ok(equipment);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{equipmentId}")
    public ResponseEntity<?> updateEquipment(@PathVariable Long equipmentId,
                                             @RequestParam Long userId, 
                                             @RequestPart("equipment") EquipmentDTO equipmentDTO,
                                             @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            EquipmentDTO updatedEquipment = equipmentService.updateEquipment(equipmentId, userId, equipmentDTO, imageFile);
            return ResponseEntity.ok(updatedEquipment);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) { 
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); 
        } catch (RuntimeException e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<?> deleteEquipment(@PathVariable Long equipmentId,
                                             @RequestParam Long userId) {
        try {
            equipmentService.deleteEquipment(equipmentId, userId);
            return ResponseEntity.ok("Equipment with ID " + equipmentId + " deleted successfully.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) { 
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Could not delete equipment. It might be associated with other records.");
            // Or a more generic internal server error:
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the equipment.");
        }
    }
}