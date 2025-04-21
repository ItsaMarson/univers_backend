package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                                          @RequestPart("image") MultipartFile imagePath){

        try{
            EquipmentDTO newEquipment = equipmentService.addEquipment(userId, equipmentDTO, imagePath);
            return  new ResponseEntity<>(newEquipment, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
