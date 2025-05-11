/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventApprovalService {

    private final EventApprovalRepository eventApprovalRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    public EventApprovalService(
            EventApprovalRepository eventApprovalRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            UserMapper userMapper) {
        this.eventApprovalRepository = eventApprovalRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
    }

    public String approveEvent(UUID eventId, String remarks) {
        String currentUsername =
                ((UserDetails)
                                SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal())
                        .getUsername();
        User currentUser =
                userRepository
                        .findByEmail(currentUsername)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Authenticated user not found in database"));

        Optional<Event> eventOpt = eventRepository.findByPublicId(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();

        if (currentUser.getRoles() == Role.SUPER_ADMIN) {
            if (event.getStatus() == Status.CANCELED) {
                return "Error: Cannot approve a canceled event.";
            }

            EventApproval eventApproval = new EventApproval();
            eventApproval.setEvent(event);
            eventApproval.setSignedBy(currentUser);
            eventApproval.setRemarks("Approved directly by SUPER_ADMIN. " + remarks);
            eventApproval.setStatus(Status.APPROVED);
            eventApproval.setDateSigned(LocalDateTime.now());
            eventApprovalRepository.save(eventApproval);

            event.setStatus(Status.APPROVED);
            eventRepository.save(event);

            User organizer = event.getOrganizer();
            if (organizer != null && organizer.getEmail() != null) {
                String messageText =
                        "Your event '"
                                + event.getEventName()
                                + "' has been approved by SUPER_ADMIN: "
                                + currentUser.getFullName()
                                + ".";
                notificationService.createNotification(
                        organizer,
                        messageText,
                        event.getPublicId(),
                        event.getPublicId(),
                        "EVENT_APPROVED");
            }
            return "Event approved directly by SUPER_ADMIN: " + currentUser.getFullName();
        }

        Role currentUserRole = currentUser.getRoles();
        if (currentUserRole == Role.VENUE_OWNER) {
            return approveByVenueOwner(eventId, currentUser, remarks);
        } else if ((currentUserRole == Role.EQUIPMENT_OWNER || currentUserRole == Role.MSDO)
                && currentUser.getDepartment() != null
                && currentUser.getDepartment().getName() != null
                && currentUser.getDepartment().getName().contains("MSDO")) {
            return approveByMSDO(eventId, currentUser, remarks);
        } else if ((currentUserRole == Role.EQUIPMENT_OWNER || currentUserRole == Role.OPC)
                && currentUser.getDepartment() != null
                && currentUser.getDepartment().getName() != null
                && currentUser.getDepartment().getName().contains("OPC")) {
            return approveByOPC(eventId, currentUser, remarks);
        } else if (currentUserRole == Role.DEPT_HEAD) {
            return approveByDepartmentHead(eventId, currentUser, remarks);
        } else if (currentUserRole == Role.VP_ADMIN) {
            return approveByVPAdmin(eventId, currentUser, remarks);
        } else if (currentUserRole == Role.VPAA) {
            return approveByVPAA(eventId, currentUser, remarks);
        } else if (currentUserRole == Role.SSD) {
            return approveBySSD(eventId, currentUser, remarks);
        } else if (currentUserRole == Role.FAO) {
            return approveByFAO(eventId, currentUser, remarks);
        } else {
            return "You are not authorized to approve this event based on your roles.";
        }
    }

    @Transactional
    public String approveByVenueOwner(UUID eventId, User approver, String remarks) {

        Optional<Event> eventOpt = eventRepository.findByPublicId(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();
        Venue venue = event.getEventVenue();

        if (venue == null) {
            return "Error: No venue is assigned to this event.";
        }
        if (venue.getVenueOwner() == null
                || !venue.getVenueOwner().getId().equals(approver.getId())) {
            return "Error: You are not the owner of this venue (" + venue.getName() + ").";
        }
        if (approver.getRoles() != Role.VENUE_OWNER) {
            return "Error: User does not have the VENUE_OWNER role.";
        }

        if (eventApprovalRepository.existsByEventAndSignedByAndStatus(
                event, approver, Status.APPROVED)) {
            return "Warning: You ("
                    + approver.getFullName()
                    + ") have already approved this event.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event, approver, Role.VENUE_OWNER);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    "Your event '"
                            + event.getEventName()
                            + "' has received approval from Venue Owner: "
                            + approver.getFullName()
                            + ".";
            notificationService.createNotification(
                    organizer,
                    notificationMessage,
                    event.getPublicId(),
                    event.getPublicId(),
                    "EVENT_APPROVED");
        }

        String eventApprovalMessage =
                "Venue approved successfully by "
                        + approver.getRoles().name()
                        + ": "
                        + approver.getFullName();
        return eventApprovalMessage;
    }

    public String approveByDepartmentHead(UUID eventId, User approver, String remarks) {
        Optional<Event> eventOpt = eventRepository.findByPublicId(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();

        if (event.getOrganizer() == null
                || event.getOrganizer().getDepartment() == null
                || event.getOrganizer().getDepartment().getDeptHead() == null) {
            return "Error: Cannot determine the required department head for this event's"
                    + " organizer.";
        }

        User requiredDeptHead = event.getOrganizer().getDepartment().getDeptHead();

        if (!requiredDeptHead.getId().equals(approver.getId())) {
            return "Error: You are not the designated department head for the organizer's"
                    + " department ("
                    + event.getOrganizer().getDepartment().getName()
                    + ").";
        }
        if (approver.getRoles() != Role.DEPT_HEAD) {
            return "Error: User does not have the DEPT_HEAD role.";
        }

        // Check if this department head has already approved this event
        if (eventApprovalRepository.existsByEventAndSignedByAndStatus(
                event, approver, Status.APPROVED)) {
            return "Warning: You (Department Head: "
                    + approver.getFullName()
                    + ") have already approved this event.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());

        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event, approver, Role.DEPT_HEAD);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    "Your event '"
                            + event.getEventName()
                            + "' has received approval from Department Head: "
                            + approver.getFullName()
                            + ".";
            notificationService.createNotification(
                    organizer,
                    notificationMessage,
                    event.getPublicId(),
                    event.getPublicId(),
                    "EVENT_APPROVED");
        }
        return "Approved successfully by Department Head: " + approver.getFullName();
    }

    public String approveByMSDO(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.MSDO);
    }

    public String approveByOPC(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.OPC);
    }

    public String approveByVPAdmin(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.VP_ADMIN);
    }

    public String approveByVPAA(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.VPAA);
    }

    public String approveBySSD(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.SSD);
    }

    public String approveByFAO(UUID eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.FAO);
    }

    public String approve(UUID eventId, User approver, String remarks, Role requiredRole) {
        Optional<Event> eventOptional = eventRepository.findByPublicId(eventId);
        if (eventOptional.isEmpty()) return "Error: Event not found";
        Event event = eventOptional.get();

        if (approver.getRoles() != requiredRole) {
            return "Error: You do not have the required role ("
                    + requiredRole.name()
                    + ") to approve this event.";
        }

        if (eventApprovalRepository.existsByEventAndSignedByAndStatus(
                event, approver, Status.APPROVED)) {
            return "Warning: You ("
                    + approver.getFullName()
                    + ") have already approved this event.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event, approver, requiredRole);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    String.format(
                            "Your event '%s' has received approval from %s: %s.",
                            event.getEventName(), requiredRole.name(), approver.getFullName());
            notificationService.createNotification(
                    organizer,
                    notificationMessage,
                    event.getPublicId(),
                    event.getPublicId(),
                    "EVENT_PARTIALLY_APPROVED");
        }

        return String.format(
                "%s approval successful by %s: %s",
                requiredRole.name(), approver.getRoles().name(), approver.getFullName());
    }

    private void checkAndUpdateEventStatus(Event event, User lastApprover, Role lastApprovalRole) {
        if (event.getStatus() == Status.APPROVED || event.getStatus() == Status.CANCELED) {
            return;
        }

        List<EventApproval> approvals =
                eventApprovalRepository.findAllByEventAndStatus(event, Status.APPROVED);

        // 1. Check for Organizer's Department Head Approval
        boolean organizerDeptHeadApproved = false;
        if (event.getOrganizer() != null
                && event.getOrganizer().getDepartment() != null
                && event.getOrganizer().getDepartment().getDeptHead() != null) {
            User requiredDeptHead = event.getOrganizer().getDepartment().getDeptHead();
            organizerDeptHeadApproved =
                    approvals.stream()
                            .anyMatch(
                                    a ->
                                            a.getSignedBy().getId().equals(requiredDeptHead.getId())
                                                    && a.getSignedBy().getRoles()
                                                            == Role.DEPT_HEAD);
        } else {
            // If no specific department head for the organizer, this condition is not met for full
            // approval based on this specific check.
            // Alternatively, if any DEPT_HEAD approval was acceptable, the logic would be:
            // organizerDeptHeadApproved = approvals.stream().anyMatch(a ->
            // a.getSignedBy().getRoles() == Role.DEPT_HEAD);
            // Sticking to specific DH for now.
            organizerDeptHeadApproved = false;
        }

        // 2. Check for Event's Venue Owner Approval
        boolean eventVenueOwnerApproved = false;
        if (event.getEventVenue() != null && event.getEventVenue().getVenueOwner() != null) {
            User requiredVenueOwner = event.getEventVenue().getVenueOwner();
            eventVenueOwnerApproved =
                    approvals.stream()
                            .anyMatch(
                                    a ->
                                            a.getSignedBy()
                                                            .getId()
                                                            .equals(requiredVenueOwner.getId())
                                                    && a.getSignedBy().getRoles()
                                                            == Role.VENUE_OWNER);
        } else {
            // If no venue is assigned to the event, or the venue has no owner,
            // this specific approval is considered not applicable/waived for the purpose of this
            // check.
            eventVenueOwnerApproved = true;
        }

        // 3. Check for MSDO-related Approval
        boolean msdoApproved =
                approvals.stream()
                        .anyMatch(
                                a -> {
                                    User signedBy = a.getSignedBy();
                                    boolean isMSDORole = signedBy.getRoles() == Role.MSDO;
                                    boolean isEquipmentOwnerInMSDODept =
                                            signedBy.getRoles() == Role.EQUIPMENT_OWNER
                                                    && signedBy.getDepartment() != null
                                                    && signedBy.getDepartment().getName() != null
                                                    && signedBy.getDepartment()
                                                            .getName()
                                                            .contains("MSDO");
                                    return isMSDORole || isEquipmentOwnerInMSDODept;
                                });

        // 4. Check for OPC-related Approval
        boolean opcApproved =
                approvals.stream()
                        .anyMatch(
                                a -> {
                                    User signedBy = a.getSignedBy();
                                    boolean isOPCRole = signedBy.getRoles() == Role.OPC;
                                    boolean isEquipmentOwnerInOPCDept =
                                            signedBy.getRoles() == Role.EQUIPMENT_OWNER
                                                    && signedBy.getDepartment() != null
                                                    && signedBy.getDepartment().getName() != null
                                                    && signedBy.getDepartment()
                                                            .getName()
                                                            .contains("OPC");
                                    return isOPCRole || isEquipmentOwnerInOPCDept;
                                });

        // 5. Check for VP_ADMIN Approval
        boolean vpAdminApproved =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.VP_ADMIN);

        // 6. Check for VPAA Approval
        boolean vpaaApproved =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.VPAA);

        // 7. Check for SSD Approval
        boolean ssdApproved =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.SSD);

        // 8. Check for FAO Approval
        boolean faoApproved =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.FAO);

        boolean isFullyApproved =
                organizerDeptHeadApproved
                        && eventVenueOwnerApproved
                        && msdoApproved
                        && opcApproved
                        && vpAdminApproved
                        && vpaaApproved
                        && ssdApproved
                        && faoApproved;

        if (isFullyApproved) {
            if (event.getStatus() != Status.APPROVED) {
                event.setStatus(Status.APPROVED);
                eventRepository.save(event);

                User organizer = event.getOrganizer();
                if (organizer != null && organizer.getEmail() != null) {
                    String message =
                            "Your event '" + event.getEventName() + "' has been fully approved.";
                    notificationService.createNotification(
                            organizer,
                            message,
                            event.getPublicId(),
                            event.getPublicId(),
                            "EVENT_FULLY_APPROVED");
                }
            }
        }
    }

    @Transactional
    public String rejectEvent(UUID eventId, String remarks) {
        String currentUsername =
                ((UserDetails)
                                SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal())
                        .getUsername();
        User currentUser =
                userRepository
                        .findByEmail(currentUsername)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Authenticated user not found in database"));

        Optional<Event> eventOpt = eventRepository.findByPublicId(eventId);
        if (eventOpt.isEmpty()) {
            return "Error: Event not found.";
        }
        Event event = eventOpt.get();

        if (event.getStatus() == Status.CANCELED || event.getStatus() == Status.APPROVED) {
            return "Error: Cannot reject an already approved or canceled event.";
        }
        if (remarks == null || remarks.trim().isEmpty()) {
            return "Error: Rejection remarks are required.";
        }

        Role rejectingRole = currentUser.getRoles();

        event.setStatus(Status.REJECTED);
        eventRepository.save(event);

        EventApproval rejectionRecord = new EventApproval();
        rejectionRecord.setEvent(event);
        rejectionRecord.setSignedBy(currentUser);
        rejectionRecord.setRemarks(remarks);
        rejectionRecord.setStatus(Status.REJECTED);
        rejectionRecord.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(rejectionRecord);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String message =
                    "Your event '"
                            + event.getEventName()
                            + "' has been rejected by "
                            + rejectingRole.name()
                            + ": "
                            + currentUser.getFullName()
                            + ". Remarks: "
                            + remarks;
            notificationService.createNotification(
                    organizer, message, event.getPublicId(), event.getPublicId(), "EVENT_REJECTED");
        }
        return "Event rejected by " + currentUser.getFullName();
    }

    public List<EventApprovalDTO> getAllApprovalsOfEvent(UUID eventId) {
        Event event =
                eventRepository
                        .findByPublicId(eventId)
                        .orElseThrow(
                                () -> new RuntimeException("Event not found with id: " + eventId));
        List<EventApproval> approvals = eventApprovalRepository.findAllByEvent(event);
        return approvals.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private EventApprovalDTO mapToDTO(EventApproval approval) {
        User signedByEntity = approval.getSignedBy();
        UserDTO signedByUserDto = null;
        String userRole = null;

        if (signedByEntity != null) {
            signedByUserDto = userMapper.toDto(signedByEntity);
            if (signedByEntity.getRoles() != null) {
                userRole = signedByEntity.getRoles().name();
            }
        }

        return new EventApprovalDTO(
                approval.getPublicId(),
                approval.getEvent().getPublicId(),
                signedByUserDto,
                userRole,
                approval.getRemarks(),
                approval.getStatus().name(),
                approval.getDateSigned());
    }
}
