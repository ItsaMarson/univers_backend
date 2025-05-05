/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public EventApprovalService(
            EventApprovalRepository eventApprovalRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.eventApprovalRepository = eventApprovalRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public String approveEvent(Long eventId, String remarks) {
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

        Optional<Event> eventOpt = eventRepository.findById(eventId);
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
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("type", "EVENT_APPROVAL_UPDATE");
                notificationPayload.put("message", messageText);
                notificationPayload.put("eventId", event.getId());
                notificationPayload.put("relatedEntityType", "EVENT");
                notificationPayload.put("eventName", event.getEventName());
                notificationPayload.put("approverName", currentUser.getFullName());
                notificationPayload.put("approverRole", Role.SUPER_ADMIN.name());
                notificationPayload.put("status", event.getStatus().name());

                notificationService.notifyUser(
                        organizer.getEmail(), "/queue/notifications", notificationPayload);
            }
            return "Event approved directly by SUPER_ADMIN: " + currentUser.getFullName();
        }

        if (currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return approveByVenueOwner(eventId, currentUser, remarks);
        } else if (currentUser.getRoles().toString().contains(Role.EQUIPMENT_OWNER.toString())) {
            // Distinguish between MSDO and OPC if they are both EQUIPMENT_OWNER
            // This logic might need refinement based on how MSDO/OPC are identified
            return approveByMSDO(
                    eventId, currentUser, remarks); // Or approveByOPC based on specific check
        } else if (currentUser.getRoles().toString().contains(Role.DEPT_HEAD.toString())) {
            return approveByDepartmentHead(eventId, currentUser, remarks);
        } else if (currentUser.getRoles().toString().contains(Role.VP_ADMIN.toString())) {
            return approveByVPAdmin(eventId, currentUser, remarks);
        } else if (currentUser.getRoles().toString().contains(Role.VPAA.toString())) {
            return approveByVPAA(eventId, currentUser, remarks);
        } else if (currentUser.getRoles().toString().contains(Role.SSD.toString())) {
            return approveBySSD(eventId, currentUser, remarks);
        } else if (currentUser.getRoles().toString().contains(Role.FAO.toString())) {
            return approveByFAO(eventId, currentUser, remarks);
        } else {
            return "You are not authorized to approve this event based on your roles.";
        }
    }

    public String approveByVenueOwner(Long eventId, User approver, String remarks) {

        Optional<Event> eventOpt = eventRepository.findById(eventId);
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
        if (!approver.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return "Error: User does not have the VENUE_OWNER role.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    "Your event '"
                            + event.getEventName()
                            + "' has received approval from Venue Owner: "
                            + approver.getFullName()
                            + ".";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_APPROVAL_UPDATE");
            payload.put("message", notificationMessage);
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("approverName", approver.getFullName());
            payload.put("approverRole", Role.VENUE_OWNER.name());
            payload.put("status", event.getStatus().name());

            notificationService.notifyUser(organizer.getEmail(), "/queue/notifications", payload);
        }

        return "Venue approved successfully by "
                + approver.getRoles()
                + ": "
                + approver.getFullName();
    }

    public String approveByDepartmentHead(Long eventId, User approver, String remarks) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);
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
        if (!approver.getRoles().toString().contains(Role.DEPT_HEAD.toString())) {
            return "Error: User does not have the DEPT_HEAD role.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());

        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    "Your event '"
                            + event.getEventName()
                            + "' has received approval from Department Head: "
                            + approver.getFullName()
                            + ".";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_APPROVAL_UPDATE");
            payload.put("message", notificationMessage);
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("approverName", approver.getFullName());
            payload.put("approverRole", Role.DEPT_HEAD.name());
            payload.put("status", event.getStatus().name());

            notificationService.notifyUser(organizer.getEmail(), "/queue/notifications", payload);
        }
        return "Approved successfully by Department Head: " + approver.getFullName();
    }

    public String approveByMSDO(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.EQUIPMENT_OWNER);
    }

    public String approveByOPC(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.EQUIPMENT_OWNER);
    }

    public String approveByVPAdmin(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.VP_ADMIN);
    }

    public String approveByVPAA(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.VPAA);
    }

    public String approveBySSD(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.SSD);
    }

    public String approveByFAO(Long eventId, User approver, String remarks) {
        return approve(eventId, approver, remarks, Role.FAO);
    }

    public String approve(Long eventId, User approver, String remarks, Role requiredRole) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty()) return "Error: Event not found";
        Event event = eventOptional.get();

        if (!approver.getRoles().toString().contains(requiredRole.toString())) {
            return "Error: You do not have the required role ("
                    + requiredRole.name()
                    + ") to approve this event.";
        }

        if (eventApprovalRepository.existsByEventAndSignedByAndStatus(
                event, approver, Status.APPROVED)) {
            return "Warning: You have already approved this event with your current role.";
        }

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(approver);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.APPROVED);
        eventApproval.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(eventApproval);

        checkAndUpdateEventStatus(event);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String notificationMessage =
                    "Your event '"
                            + event.getEventName()
                            + "' has received approval from "
                            + requiredRole.name()
                            + ": "
                            + approver.getFullName()
                            + ".";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_APPROVAL_UPDATE");
            payload.put("message", notificationMessage);
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("approverName", approver.getFullName());
            payload.put("approverRole", requiredRole.name());
            payload.put("status", event.getStatus().name());

            notificationService.notifyUser(organizer.getEmail(), "/queue/notifications", payload);
        }

        return "Approved successfully by " + requiredRole.name() + ": " + approver.getFullName();
    }

    // Helper method to check approvals and update event status
    private void checkAndUpdateEventStatus(Event event) {
        // Skip if already approved (e.g., by SUPER_ADMIN) or canceled
        if (event.getStatus() == Status.APPROVED || event.getStatus() == Status.CANCELED) {
            return;
        }

        List<EventApproval> approvals =
                eventApprovalRepository.findAllByEventAndStatus(event, Status.APPROVED);
        // Define the required roles for the event to be fully APPROVED
        boolean hasDeptHeadApproval =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.DEPT_HEAD);
        boolean hasVenueOwnerApproval =
                approvals.stream().anyMatch(a -> a.getSignedBy().getRoles() == Role.VENUE_OWNER);
        // Add checks for MSDO, OPC, VP_ADMIN, VPAA, SSD, FAO as needed...
        boolean hasMSDOApproval =
                approvals.stream()
                        .anyMatch(a -> a.getSignedBy().getRoles() == Role.EQUIPMENT_OWNER);

        if (hasDeptHeadApproval
                && hasVenueOwnerApproval
                && hasMSDOApproval /* && other required approvals */) {

            if (event.getStatus() != Status.APPROVED) {
                event.setStatus(Status.APPROVED);
                eventRepository.save(event);

                User organizer = event.getOrganizer();
                if (organizer != null && organizer.getEmail() != null) {
                    String notificationMessage =
                            "Your event '" + event.getEventName() + "' is now fully APPROVED.";
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "EVENT_FULLY_APPROVED");
                    payload.put("message", notificationMessage);
                    payload.put("eventId", event.getId());
                    payload.put("relatedEntityType", "EVENT");
                    payload.put("eventName", event.getEventName());
                    payload.put("status", Status.APPROVED.name());

                    notificationService.notifyUser(
                            organizer.getEmail(), "/queue/notifications", payload);
                }
            }
        }
        // Add logic for other statuses if needed (e.g., PARTIALLY_APPROVED)
    }

    @Transactional
    public String rejectEvent(Long eventId, String remarks) {
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

        Optional<Event> eventOpt = eventRepository.findById(eventId);
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

        // Determine the role under which the user is rejecting (this might need refinement)
        // For simplicity, let's assume the primary role or a relevant role is used.
        Role rejectingRole = currentUser.getRoles(); // Or determine more specifically

        EventApproval eventApproval = new EventApproval();
        eventApproval.setEvent(event);
        eventApproval.setSignedBy(currentUser);
        eventApproval.setRemarks(remarks);
        eventApproval.setStatus(Status.REJECTED);
        eventApproval.setDateSigned(LocalDateTime.now());
        eventApprovalRepository.save(eventApproval);

        event.setStatus(Status.REJECTED);
        eventRepository.save(event);

        User organizer = event.getOrganizer();
        if (organizer != null && organizer.getEmail() != null) {
            String messageText =
                    "Your event '"
                            + event.getEventName()
                            + "' has been REJECTED by "
                            + rejectingRole.name()
                            + ": "
                            + currentUser.getFullName()
                            + ". Reason: "
                            + remarks;
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_REJECTED");
            payload.put("message", messageText);
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("rejectorName", currentUser.getFullName());
            payload.put("rejectorRole", rejectingRole.name());
            payload.put("remarks", remarks);
            payload.put("status", Status.REJECTED.name());

            notificationService.notifyUser(organizer.getEmail(), "/queue/notifications", payload);
        }

        return "Event rejected successfully by "
                + rejectingRole.name()
                + ": "
                + currentUser.getFullName();
    }

    public List<EventApprovalDTO> getAllApprovalsOfEvent(Long eventId) {

        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isEmpty()) {
            throw new IllegalArgumentException("Event not found");
        }
        Event event = eventOptional.get();
        List<EventApproval> eventApprovals = eventApprovalRepository.findAllByEvent(event);

        return eventApprovals.stream()
                .map(
                        approval -> {
                            User signedByUser = approval.getSignedBy();
                            return new EventApprovalDTO(
                                    approval.getId(),
                                    approval.getEvent().getId(),
                                    signedByUser.getId(),
                                    signedByUser.getRoles().name(),
                                    signedByUser.getDepartment() != null
                                            ? signedByUser.getDepartment().getName()
                                            : "N/A",
                                    signedByUser.getFullName(),
                                    approval.getRemarks(),
                                    approval.getStatus().toString(),
                                    approval.getDateSigned());
                        })
                .collect(Collectors.toList());
    }
}
