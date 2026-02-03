/* (C)2025-2026 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EquipmentChecklistRequest;
import com.univers.univers_backend.DTO.EquipmentChecklistStatusDTO;
import com.univers.univers_backend.Enum.Task;
import com.univers.univers_backend.Service.EquipmentChecklistService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/equipment-checklist")
@Tag(name = "Equipment Checklist", description = "APIs for managing equipment checklist")
public class EquipmentChecklistController {

    private final EquipmentChecklistService checklistService;

    public EquipmentChecklistController(EquipmentChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<String>>> getAssignedEquipment(
            @RequestParam UUID eventPersonnelId) {
        List<String> assigned = checklistService.getAssignedEquipment(eventPersonnelId);
        return ResponseEntity.ok(ApiResponse.success(assigned));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<String>> submitChecklist(
            @RequestBody EquipmentChecklistRequest request) {
        checklistService.submitChecklist(request);
        return ResponseEntity.ok(ApiResponse.success("Checklist submitted"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<String>>> getChecklistStatus(
            @RequestParam UUID eventId, @RequestParam Task task) {
        List<String> checkedEquipment = checklistService.getCheckedEquipmentForEvent(eventId, task);
        return ResponseEntity.ok(ApiResponse.success(checkedEquipment));
    }

    @GetMapping("/status/detail")
    public ResponseEntity<ApiResponse<List<EquipmentChecklistStatusDTO>>>
            getDetailedChecklistStatus(@RequestParam UUID eventId, @RequestParam Task task) {
        List<EquipmentChecklistStatusDTO> status =
                checklistService.getDetailedChecklistStatus(eventId, task);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
