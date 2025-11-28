/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentChecklistRequest;
import com.univers.univers_backend.DTO.EquipmentChecklistStatusDTO;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.Entity.EquipmentChecklist;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventPersonnel;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Enum.Task;
import com.univers.univers_backend.Repository.EquipmentChecklistRepository;
import com.univers.univers_backend.Repository.EventPersonnelRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentChecklistService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentChecklistService.class);

    private final EventPersonnelRepository personnelRepository;
    private final EquipmentChecklistRepository equipmentChecklistRepository;
    private final EquipmentReservationService equipmentReservationService;

    public EquipmentChecklistService(
            EventPersonnelRepository personnelRepository,
            EquipmentReservationService equipmentReservationService,
            EquipmentChecklistRepository equipmentChecklistRepository) {
        this.personnelRepository = personnelRepository;
        this.equipmentReservationService = equipmentReservationService;
        this.equipmentChecklistRepository = equipmentChecklistRepository;
    }

    public List<String> getAssignedEquipment(UUID eventPersonnelId) {
        EventPersonnel personnel =
                personnelRepository
                        .findByPublicId(eventPersonnelId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event personnel not found with public id: "
                                                        + eventPersonnelId));

        UUID evenPublicId = personnel.getEvent().getPublicId();

        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getReservationsByEventPublicId(evenPublicId);

        List<String> allEquipment =
                reservations.stream().map(res -> res.equipment().publicId().toString()).toList();
        List<String> personnelEquipment = personnel.getAssignedEquipmentIds();

        return allEquipment.stream().filter(personnelEquipment::contains).toList();
    }

    @Transactional
    public void submitChecklist(EquipmentChecklistRequest request) {

        EventPersonnel personnel =
                personnelRepository
                        .findByPublicId(request.eventPersonnelId())
                        .orElseThrow(
                                () -> new NoSuchElementException("Assigned personnel not found"));

        // Validate submitted equipment: allow partial, but must be assigned
        List<String> assignedEquipmentIds =
                personnel.getAssignedEquipmentIds() != null
                        ? personnel.getAssignedEquipmentIds()
                        : List.of();
        List<String> submittedEquipmentIds = request.equipmentIds();

        if (submittedEquipmentIds == null || submittedEquipmentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one equipment item must be checked before submitting the checklist.");
        }

        if (!assignedEquipmentIds.containsAll(submittedEquipmentIds)) {
            throw new IllegalArgumentException(
                    "Checklist contains equipment that is not assigned to this personnel.");
        }

        // Create and save the checklist
        EquipmentChecklist checklist = new EquipmentChecklist();
        checklist.setEventPersonnel(personnel);
        checklist.setEquipmentIds(request.equipmentIds());
        checklist.setTask(personnel.getTask());
        equipmentChecklistRepository.save(checklist);

        logger.info(
                "Checklist submitted by personnel {} for task {}",
                personnel.getPublicId(),
                personnel.getTask());

        // If task is PULLOUT, restore equipment to inventory
        if (personnel.getTask() == Task.PULLOUT) {
            UUID eventPublicId = personnel.getEvent().getPublicId();
            List<String> checkedEquipmentIds = request.equipmentIds();

            // Get all reservations for this event
            List<EquipmentReservationDTO> reservations =
                    equipmentReservationService.getReservationsByEventPublicId(eventPublicId);

            int completedReservations = 0;

            // For each checked equipment, find and complete its reservation
            for (String equipmentId : checkedEquipmentIds) {
                reservations.stream()
                        .filter(
                                res ->
                                        res.equipment().publicId().toString().equals(equipmentId)
                                                && ("APPROVED".equals(res.status())
                                                        || "ONGOING".equals(res.status())))
                        .forEach(
                                res -> {
                                    equipmentReservationService.completeReservation(res.publicId());
                                    logger.info(
                                            "Equipment {} returned to inventory via PULLOUT task",
                                            equipmentId);
                                });
                completedReservations++;
            }

            // Mark submitting personnel task as completed
            personnel.setStatus(Status.COMPLETED);
            personnelRepository.save(personnel);

            logger.info(
                    "PULLOUT task completed. {} equipment items returned to inventory for event {}",
                    completedReservations,
                    eventPublicId);
        } else if (personnel.getTask() == Task.SETUP) {
            // For SETUP task, mark submitting personnel as completed
            personnel.setStatus(Status.COMPLETED);
            personnelRepository.save(personnel);

            logger.info(
                    "SETUP task completed by personnel {} for event {}",
                    personnel.getPublicId(),
                    personnel.getEvent().getPublicId());
        }

        // After saving this checklist, check if ALL event equipment has been checked (globally)
        Event event = personnel.getEvent();
        if (event != null) {
            UUID eventPublicId = event.getPublicId();

            // Full equipment set for this event (based on reservations)
            List<EquipmentReservationDTO> eventReservations =
                    equipmentReservationService.getReservationsByEventPublicId(eventPublicId);
            Set<String> allEventEquipmentIds =
                    eventReservations.stream()
                            .filter(
                                    res ->
                                            res.equipment() != null
                                                    && res.equipment().publicId() != null)
                            .map(res -> res.equipment().publicId().toString())
                            .collect(Collectors.toSet());

            if (!allEventEquipmentIds.isEmpty()) {
                // Union of all equipmentIds from all checklists for this event and this task
                List<EquipmentChecklist> allChecklistsForEventAndTask =
                        equipmentChecklistRepository.findByEventPersonnel_Event_PublicIdAndTask(
                                eventPublicId, personnel.getTask());

                Set<String> globallyCheckedEquipmentIds = new HashSet<>();
                for (EquipmentChecklist ec : allChecklistsForEventAndTask) {
                    if (ec.getEquipmentIds() != null) {
                        globallyCheckedEquipmentIds.addAll(ec.getEquipmentIds());
                    }
                }

                // If every event equipment ID appears in at least one checklist, complete ALL
                // personnel for this event & task
                if (globallyCheckedEquipmentIds.containsAll(allEventEquipmentIds)) {
                    List<EventPersonnel> allPersonnelForEvent = event.getAssignedPersonnel();
                    if (allPersonnelForEvent != null) {
                        for (EventPersonnel ep : allPersonnelForEvent) {
                            if (ep.getTask() == personnel.getTask()
                                    && ep.getStatus() != Status.COMPLETED) {
                                ep.setStatus(Status.COMPLETED);
                                personnelRepository.save(ep);
                            }
                        }
                    }

                    logger.info(
                            "All equipment for event {} and task {} has been checked. Marked all"
                                    + " personnel as COMPLETED.",
                            eventPublicId,
                            personnel.getTask());
                }
            }
        }
    }

    public List<String> getCheckedEquipmentForEvent(UUID eventPublicId, Task task) {
        List<EquipmentChecklist> checklists =
                equipmentChecklistRepository.findByEventPersonnel_Event_PublicIdAndTask(
                        eventPublicId, task);

        return checklists.stream()
                .filter(ec -> ec.getEquipmentIds() != null)
                .flatMap(ec -> ec.getEquipmentIds().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<EquipmentChecklistStatusDTO> getDetailedChecklistStatus(
            UUID eventPublicId, Task task) {
        List<EquipmentReservationDTO> reservations =
                equipmentReservationService.getReservationsByEventPublicId(eventPublicId);

        // Build full set of equipment for the event
        Set<String> allEventEquipmentIds =
                reservations.stream()
                        .filter(
                                res ->
                                        res.equipment() != null
                                                && res.equipment().publicId() != null)
                        .map(res -> res.equipment().publicId().toString())
                        .collect(Collectors.toSet());

        // Aggregate which personnel checked which equipment
        List<EquipmentChecklist> checklists =
                equipmentChecklistRepository.findByEventPersonnel_Event_PublicIdAndTask(
                        eventPublicId, task);

        // equipmentId -> set of personnel publicIds
        Map<String, Set<UUID>> equipmentToPersonnel = new HashMap<>();
        for (EquipmentChecklist checklist : checklists) {
            if (checklist.getEquipmentIds() == null || checklist.getEventPersonnel() == null) {
                continue;
            }
            UUID personnelId = checklist.getEventPersonnel().getPublicId();
            for (String equipmentId : checklist.getEquipmentIds()) {
                equipmentToPersonnel
                        .computeIfAbsent(equipmentId, key -> new HashSet<>())
                        .add(personnelId);
            }
        }

        return allEventEquipmentIds.stream()
                .map(
                        id ->
                                new EquipmentChecklistStatusDTO(
                                        id,
                                        equipmentToPersonnel.containsKey(id),
                                        equipmentToPersonnel.containsKey(id)
                                                ? List.copyOf(equipmentToPersonnel.get(id))
                                                : List.of()))
                .collect(Collectors.toList());
    }
}
