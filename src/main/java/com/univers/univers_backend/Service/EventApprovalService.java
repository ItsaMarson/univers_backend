/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(EventApprovalService.class);

    private final EventApprovalRepository eventApprovalRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final EquipmentReservationService equipmentReservationService;

    public EventApprovalService(
            EventApprovalRepository eventApprovalRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            UserMapper userMapper,
            EquipmentReservationService equipmentReservationService) {
        this.eventApprovalRepository = eventApprovalRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.equipmentReservationService = equipmentReservationService;
    }

    // Helper method to get the current authenticated user
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new IllegalStateException(
                    "Principal is not of expected type (UserDetails or String).");
        }
        return userRepository
                .findByEmail(username)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User not found with email: " + username));
    }

    @Transactional
    public EventApprovalDTO processApprovalAction(
            UUID eventPublicId, Status newStatus, String remarks) {
        User currentUser = getCurrentUser();

        Event event =
                eventRepository
                        .findByPublicId(eventPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with ID: " + eventPublicId));

        // Find the EventApproval record for this event and the current user
        EventApproval eventApproval =
                eventApprovalRepository
                        .findByEventAndSignedBy(event, currentUser)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No pending approval found for user "
                                                        + currentUser.getPublicId()
                                                        + " on event "
                                                        + eventPublicId));

        // The check for ownership is implicitly handled by finding the approval signed by the
        // current user.
        // If no record is found for the current user and this event, the above orElseThrow is
        // triggered.
        // We still need to check if an approval record exists but might belong to a different user
        // if the query `findByEventAndSignedBy` was broader. However, given its name, it should be
        // specific.
        // For safety, let's ensure the found approval indeed matches the current user, though it
        // should be redundant
        // if `findByEventAndSignedBy` is correctly implemented and used.
        if (!eventApproval.getSignedBy().getId().equals(currentUser.getId())) {
            // This case should ideally not be reached if findByEventAndSignedBy is specific.
            logger.warn(
                    "Mismatch: Found approval {} for event {} but it is signed by {} instead of"
                            + " current user {}.",
                    eventApproval.getPublicId(),
                    eventPublicId,
                    eventApproval.getSignedBy().getPublicId(),
                    currentUser.getPublicId());
            throw new SecurityException(
                    "Approval record does not belong to the current user for this event.");
        }

        if (eventApproval.getStatus() == Status.ONGOING
                || eventApproval.getStatus() == Status.COMPLETED
                || eventApproval.getStatus() == Status.CANCELED) {
            throw new IllegalStateException(
                    "This approval item has already been processed. Current status: "
                            + eventApproval.getStatus());
        }

        if (newStatus != Status.APPROVED
                && newStatus != Status.REJECTED
                && newStatus != Status.RESERVED
                && newStatus != Status.DENIED_RESERVATION
                && newStatus != Status.PAID
                && newStatus != Status.UNPAID
                && newStatus != Status.RECOMMENDED) {
            throw new IllegalArgumentException("Invalid target status for approval action.");
        }

        eventApproval.setStatus(newStatus);
        eventApproval.setRemarks(remarks);
        eventApproval.setDateSigned(Instant.now());
        EventApproval updatedApproval = eventApprovalRepository.save(eventApproval);

        // Notify the event organizer about the approval action
        User organizer = event.getOrganizer();
        if (organizer != null) {
            String approverRole =
                    currentUser.getRoles().stream()
                            .findFirst()
                            .map(role -> role.name())
                            .orElse("Approver");

            String message =
                    String.format(
                            "Your event '%s' has been %s by %s (%s).%s",
                            event.getEventName(),
                            newStatus == Status.APPROVED ? "approved" : "rejected",
                            currentUser.getFullName(),
                            approverRole,
                            remarks != null && !remarks.isBlank() ? " Remarks: " + remarks : "");

            notificationService.createNotification(
                    organizer,
                    message,
                    event.getPublicId(),
                    event.getPublicId(),
                    "EVENT_APPROVAL_ACTION");
        }

        // If this is an equipment owner approving the event, automatically approve their equipment
        // reservations
        if (newStatus == Status.APPROVED && currentUser.getRoles().contains(Role.EQUIPMENT_OWNER)) {
            try {
                // Get all equipment reservations for this event that are owned by the current user
                List<EquipmentReservationDTO> reservations =
                        equipmentReservationService.getReservationsByEventPublicId(eventPublicId);
                List<UUID> pendingReservationIds =
                        reservations.stream()
                                .filter(r -> r.status().equals("PENDING"))
                                .filter(
                                        r ->
                                                r.equipment()
                                                        .equipmentOwner()
                                                        .publicId()
                                                        .equals(currentUser.getPublicId()))
                                .map(EquipmentReservationDTO::publicId)
                                .collect(Collectors.toList());

                if (!pendingReservationIds.isEmpty()) {
                    logger.info(
                            "Equipment owner {} is approving event {}. Automatically approving {}"
                                    + " equipment reservations.",
                            currentUser.getPublicId(),
                            eventPublicId,
                            pendingReservationIds.size());
                    equipmentReservationService.bulkApproveReservations(
                            pendingReservationIds,
                            "Automatically approved as part of event approval");
                }
            } catch (Exception e) {
                // Log the error but don't fail the event approval
                logger.error(
                        "Error while automatically approving equipment reservations for event {}:"
                                + " {}",
                        eventPublicId,
                        e.getMessage(),
                        e);
            }
        }

        // If this is an equipment owner rejecting the event, automatically reject their equipment
        // reservations
        if (newStatus == Status.REJECTED && currentUser.getRoles().contains(Role.EQUIPMENT_OWNER)) {
            try {
                // Get all equipment reservations for this event that are owned by the current user
                List<EquipmentReservationDTO> reservations =
                        equipmentReservationService.getReservationsByEventPublicId(eventPublicId);
                List<UUID> reservationIdsToReject =
                        reservations.stream()
                                .filter(
                                        r ->
                                                !r.status().equals("REJECTED")
                                                        && !r.status().equals("CANCELED"))
                                .filter(
                                        r ->
                                                r.equipment()
                                                        .equipmentOwner()
                                                        .publicId()
                                                        .equals(currentUser.getPublicId()))
                                .map(EquipmentReservationDTO::publicId)
                                .collect(Collectors.toList());

                if (!reservationIdsToReject.isEmpty()) {
                    logger.info(
                            "Equipment owner {} is rejecting event {}. Automatically rejecting {}"
                                    + " equipment reservations (including approved/reserved ones).",
                            currentUser.getPublicId(),
                            eventPublicId,
                            reservationIdsToReject.size());
                    String rejectionReason =
                            remarks != null && !remarks.isBlank()
                                    ? "Automatically rejected as part of event rejection: "
                                            + remarks
                                    : "Automatically rejected as part of event rejection";
                    equipmentReservationService.bulkRejectReservations(
                            reservationIdsToReject, rejectionReason);
                }
            } catch (Exception e) {
                // Log the error but don't fail the event approval
                logger.error(
                        "Error while automatically rejecting equipment reservations for event {}:"
                                + " {}",
                        eventPublicId,
                        e.getMessage(),
                        e);
            }
        }

        checkAndUpdateEventStatus(updatedApproval.getEvent());

        return mapToDTO(updatedApproval);
    }

    private void checkAndUpdateEventStatus(Event event) {
        List<EventApproval> approvals = eventApprovalRepository.findAllByEvent(event);

        if (approvals.isEmpty()) {
            logger.warn(
                    "No approval records found for event ID: {}. Cannot determine overall status.",
                    event.getPublicId());
            // If event is PENDING and has no required approvers, it could be auto-approved.
            // This needs clear business rules. For now, if no approval records exist (e.g. for an
            // old event or misconfiguration)
            // and status is PENDING, it will remain PENDING by this logic.
            // If EventService correctly creates placeholders, this list should not be empty for
            // events requiring approval.
            return;
        }

        boolean allApproved = true;
        boolean anyRejected = false;

        for (EventApproval approval : approvals) {
            if (approval.getStatus() == Status.REJECTED
                    || approval.getStatus() == Status.DENIED_RESERVATION
                    || approval.getStatus() == Status.NOT_RECOMMENDED) {
                anyRejected = true;
                break;
            }

            if (approval.getStatus() != Status.APPROVED
                    || approval.getStatus() == Status.RESERVED
                    || approval.getStatus() == Status.RECOMMENDED) {
                // Any non-approved (and not rejected) means not all are approved yet
                allApproved = false;
            }
        }

        Status oldEventStatus = event.getStatus();
        Status newEventStatus = oldEventStatus;

        if (anyRejected) {
            newEventStatus = Status.REJECTED;
        } else if (allApproved) {
            newEventStatus = Status.APPROVED;
        } else {
            // If not all are approved and none are rejected, it's still PENDING
            newEventStatus = Status.PENDING;
        }

        if (newEventStatus != oldEventStatus) {
            event.setStatus(newEventStatus);
            eventRepository.save(event);
            logger.info(
                    "Event {} status updated from {} to {} due to approval processing.",
                    event.getPublicId(),
                    oldEventStatus,
                    newEventStatus);

            User organizer = event.getOrganizer();
            if (organizer != null) {
                String baseMessage =
                        String.format(
                                "The status of your event '%s' has been updated to %s.",
                                event.getEventName(), newEventStatus.toString());
                String finalMessage = baseMessage;

                if (newEventStatus == Status.REJECTED) {
                    String rejectionReason =
                            approvals.stream()
                                    .filter(
                                            a ->
                                                    a.getStatus() == Status.REJECTED
                                                            && a.getRemarks() != null
                                                            && !a.getRemarks().isBlank())
                                    .map(EventApproval::getRemarks)
                                    .findFirst()
                                    .orElse("No specific reason provided.");
                    finalMessage = baseMessage + " Reason: " + rejectionReason;
                }

                notificationService.createNotification(
                        organizer,
                        finalMessage,
                        event.getPublicId(),
                        event.getPublicId(),
                        "EVENT_STATUS_UPDATE");
            }
        }
    }

    public List<EventApprovalDTO> getAllApprovalsOfEvent(UUID eventId) {
        Event event =
                eventRepository
                        .findByPublicId(eventId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: " + eventId));
        List<EventApproval> approvals = eventApprovalRepository.findAllByEvent(event);
        return approvals.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private EventApprovalDTO mapToDTO(EventApproval approval) {
        if (approval == null) return null;

        UserDTO signedByUserDTO = null;
        String userRoleStr = null;
        if (approval.getSignedBy() != null) {
            signedByUserDTO = userMapper.toDto(approval.getSignedBy());
            if (approval.getSignedBy().getRoles() != null
                    && !approval.getSignedBy().getRoles().isEmpty()) {
                userRoleStr = approval.getSignedBy().getRoles().iterator().next().name();
            }
        }

        return new EventApprovalDTO(
                approval.getPublicId(),
                approval.getEvent() != null ? approval.getEvent().getPublicId() : null,
                signedByUserDTO,
                userRoleStr,
                approval.getRemarks(),
                approval.getStatus() != null ? approval.getStatus().name() : null,
                approval.getDateSigned());
    }

    @Transactional
    public List<EventApprovalDTO> processBulkApprovalAction(
            List<UUID> eventPublicIds, Status newStatus, String remarks) {
        List<EventApprovalDTO> successfulActions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (UUID eventPublicId : eventPublicIds) {
            try {
                EventApprovalDTO result = processApprovalAction(eventPublicId, newStatus, remarks);
                successfulActions.add(result);
            } catch (NoSuchElementException e) {
                logger.warn(
                        "Skipping event ID {} in bulk action: Not found - {}",
                        eventPublicId,
                        e.getMessage());
                errors.add(
                        "Event ID "
                                + eventPublicId
                                + ": Not found or no approval record for user.");
            } catch (SecurityException e) {
                logger.warn(
                        "Skipping event ID {} in bulk action: Forbidden - {}",
                        eventPublicId,
                        e.getMessage());
                errors.add("Event ID " + eventPublicId + ": User not authorized.");
            } catch (IllegalStateException | IllegalArgumentException e) {
                logger.warn(
                        "Skipping event ID {} in bulk action: Invalid state/argument - {}",
                        eventPublicId,
                        e.getMessage());
                errors.add("Event ID " + eventPublicId + ": " + e.getMessage());
            } catch (Exception e) {
                logger.error(
                        "Skipping event ID {} in bulk action: Unexpected error - {}",
                        eventPublicId,
                        e.getMessage(),
                        e);
                errors.add("Event ID " + eventPublicId + ": Unexpected error - " + e.getMessage());
            }
        }

        // If there were any errors, we might want to throw a custom exception
        // to indicate partial success/failure, or handle it as per business requirements.
        // For now, we'll log errors and return only successful ones.
        // The @Transactional annotation ensures that if an unhandled RuntimeException occurs
        // (e.g., database issue not caught above), the whole transaction rolls back.
        if (!errors.isEmpty()) {
            // This is a simple way to communicate partial failure.
            // A more robust solution might involve a custom response DTO with successes and
            // failures.
            throw new RuntimeException(
                    "Bulk action completed with errors: " + String.join("; ", errors));
        }

        return successfulActions;
    }
}
