package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.EquipmentChecklistRequest;
import com.univers.univers_backend.Service.EquipmentChecklistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/equipment-checklist")
@Tag(name = "Equipment Checklist", description = "APIs for managing equipment checklist")
public class EquipmentChecklistController {


    private final EquipmentChecklistService  checklistService;

    public EquipmentChecklistController(EquipmentChecklistService checklistService){
        this.checklistService = checklistService;
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<String>> getAssignedEquipment(@RequestParam UUID eventPersonnelId){
        List<String> assigned = checklistService.getAssignedEquipment(eventPersonnelId);
        return ResponseEntity.ok(assigned);
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitChecklist(
            @RequestBody EquipmentChecklistRequest request
            ){
        checklistService.submitChecklist(request);
        return  ResponseEntity.ok("Checklist submitted");
    }
}
