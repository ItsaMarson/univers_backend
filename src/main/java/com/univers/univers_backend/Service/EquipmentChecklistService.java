package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentChecklistRequest;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.Entity.EquipmentChecklist;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.EventPersonnel;
import com.univers.univers_backend.Repository.EquipmentChecklistRepository;
import com.univers.univers_backend.Repository.EventPersonnelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EquipmentChecklistService {

    private final EventPersonnelRepository personnelRepository;
    private final EquipmentChecklistRepository equipmentChecklistRepository;
    private final EquipmentReservationService equipmentReservationService;

    public EquipmentChecklistService(
            EventPersonnelRepository personnelRepository,
            EquipmentReservationService equipmentReservationService,
            EquipmentChecklistRepository equipmentChecklistRepository){
        this.personnelRepository = personnelRepository;
        this.equipmentReservationService = equipmentReservationService;
        this.equipmentChecklistRepository = equipmentChecklistRepository;
    }
    public List<String> getAssignedEquipment(UUID eventPersonnelId){
        EventPersonnel personnel = personnelRepository.findByPublicId(eventPersonnelId)
                .orElseThrow(() -> new NoSuchElementException("Event personnel not found with public id: " + eventPersonnelId ));

        UUID evenPublicId = personnel.getEvent().getPublicId();

        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getReservationsByEventPublicId(evenPublicId);

        List<String> allEquipment = reservations.stream()
                .map(res -> res.equipment().publicId().toString())
                .toList();
        List<String> personnelEquipment = personnel.getAssignedEquipmentIds();

        return allEquipment.stream()
                .filter(personnelEquipment::contains)
                .toList();
    }
    public void submitChecklist(EquipmentChecklistRequest request){

        EventPersonnel personnel = personnelRepository.findByPublicId(request.eventPersonnelId())
                .orElseThrow(() -> new NoSuchElementException("Assigned personnel not found"));

        EquipmentChecklist checklist = new EquipmentChecklist();
        checklist.setEventPersonnel(personnel);
        checklist.setEquipmentIds(request.equipmentIds());
        checklist.setTask(personnel.getTask());

        equipmentChecklistRepository.save(checklist);
    }
}



