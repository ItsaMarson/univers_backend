/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueApprovalDTO;
import com.univers.univers_backend.DTO.VenueReservationDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Entity.VenueApproval;
import com.univers.univers_backend.Entity.VenueReservation;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueApprovalRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.Repository.VenueReservationRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueReservationService {

    private final VenueReservationRepository venueReservationRepository;
    private final VenueApprovalRepository venueApprovalRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    private static final Set<Role> VENUE_APPROVER_ROLES =
            Set.of(
                    Role.VENUE_OWNER,
                    Role.OPC,
                    Role.MSDO,
                    Role.VP_ADMIN,
                    Role.VPAA,
                    Role.FAO,
                    Role.SSD);

    private static final Set<Role> REQUIRED_APPROVAL_ROLES =
            Set.of(
                    Role.VENUE_OWNER,
                    Role.OPC,
                    Role.MSDO,
                    Role.VP_ADMIN,
                    Role.VPAA,
                    Role.FAO,
                    Role.SSD);

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    public VenueReservationService(
            VenueReservationRepository venueReservationRepository,
            VenueApprovalRepository venueApprovalRepository,
            EventRepository eventRepository,
            VenueRepository venueRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            FileStorageService fileStorageService,
            NotificationService notificationService /* ... other dependencies */) {
        this.venueReservationRepository = venueReservationRepository;
        this.venueApprovalRepository = venueApprovalRepository;
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public VenueReservationDTO createVenueReservation(VenueReservationDTO reservationDTO) {
        User requestingUser = getCurrentUser();

        Event event =
                eventRepository
                        .findById(reservationDTO.eventId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Associated Event not found with ID: "
                                                        + reservationDTO.eventId()));

        Venue venue =
                venueRepository
                        .findById(reservationDTO.venueId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with ID: "
                                                        + reservationDTO.venueId()));

        Long deptId =
                reservationDTO.departmentId() != null
                        ? reservationDTO.departmentId()
                        : requestingUser.getDepartment().getId();
        Department department =
                departmentRepository
                        .findById(deptId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Department not found with ID: " + deptId));

        LocalDateTime startTime =
                reservationDTO.startTime() != null
                        ? reservationDTO.startTime()
                        : event.getStartTime();
        LocalDateTime endTime =
                reservationDTO.endTime() != null ? reservationDTO.endTime() : event.getEndTime();

        List<VenueReservation> conflicting =
                venueReservationRepository.findConflictingReservations(
                        venue.getId(), startTime, endTime);
        if (!conflicting.isEmpty()) {
            throw new IllegalArgumentException(
                    "Venue is already reserved for the requested time slot.");
        }

        VenueReservation newReservation = new VenueReservation();
        newReservation.setEvent(event);
        newReservation.setRequestingUser(requestingUser);
        newReservation.setDepartment(department);
        newReservation.setVenue(venue);
        newReservation.setStartTime(startTime);
        newReservation.setEndTime(endTime);

        VenueReservation savedReservation = venueReservationRepository.save(newReservation);

        notifyVenueOwner(savedReservation);

        return mapToDTO(savedReservation);
    }

    public List<VenueReservationDTO> getAllReservations() {
        return venueReservationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VenueReservationDTO getReservationById(Long reservationId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));
        return mapToDTO(reservation);
    }

    public List<VenueReservationDTO> getOwnVenueReservations() {
        User currentUser = getCurrentUser();
        List<VenueReservation> reservations =
                venueReservationRepository.findByRequestingUser(currentUser);
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public String approveReservation(Long reservationId, String remarks) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not in PENDING state (Current: "
                    + reservation.getStatus()
                    + ").";
        }

        Role currentUserRole = currentUser.getRoles();
        if (!VENUE_APPROVER_ROLES.contains(currentUserRole)) {
            return "Error: You do not have the required role to approve this venue reservation.";
        }

        if (currentUserRole == Role.VENUE_OWNER) {
            User venueOwner = reservation.getVenue().getVenueOwner();
            if (venueOwner == null) {
                return "Error: Venue Owner not assigned to the venue.";
            }
            if (!currentUser.getId().equals(venueOwner.getId())) {
                return "Error: You are not the designated Venue Owner for this venue.";
            }
        }

        if (venueApprovalRepository.existsByVenueReservationAndSignedByAndStatus(
                reservation, currentUser, Status.APPROVED)) {
            return "Warning: You have already approved this reservation.";
        }

        VenueApproval approval = new VenueApproval();
        approval.setVenueReservation(reservation);
        approval.setSignedBy(currentUser);
        approval.setStatus(Status.APPROVED);
        approval.setRemarks(remarks);

        venueApprovalRepository.save(approval);

        checkAndUpdateVenueReservationStatus(reservation);

        notifyRequester(
                reservation,
                "received approval from " + currentUserRole.name(),
                currentUser,
                "VENUE_RESERVATION_APPROVED");

        return "Venue reservation approved successfully by "
                + currentUserRole.name()
                + ": "
                + currentUser.getFullName();
    }

    @Transactional
    public String rejectReservation(Long reservationId, String remarks) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));

        if (reservation.getStatus() != Status.PENDING
                && reservation.getStatus() != Status.APPROVED) {
            if (reservation.getStatus() != Status.PENDING) {
                return "Error: Reservation is not in PENDING state (Current: "
                        + reservation.getStatus()
                        + "). Cannot reject.";
            }
        }

        Role currentUserRole = currentUser.getRoles(); // Assuming single role
        if (!VENUE_APPROVER_ROLES.contains(currentUserRole)) {
            return "Error: You do not have the required role to reject this venue reservation.";
        }

        if (currentUserRole == Role.VENUE_OWNER) {
            User venueOwner = reservation.getVenue().getVenueOwner();
            if (venueOwner == null) {
            } else if (!currentUser.getId().equals(venueOwner.getId())) {
                return "Error: You are not the designated Venue Owner for this venue.";
            }
        }

        VenueApproval rejectionRecord = new VenueApproval();
        rejectionRecord.setVenueReservation(reservation);
        rejectionRecord.setSignedBy(currentUser);
        rejectionRecord.setStatus(Status.REJECTED);
        rejectionRecord.setRemarks(remarks);
        venueApprovalRepository.save(rejectionRecord);

        reservation.setStatus(Status.REJECTED);
        venueReservationRepository.save(reservation);

        notifyRequester(reservation, "rejected", currentUser, "VENUE_RESERVATION_REJECTED");

        return "Venue reservation rejected successfully by "
                + currentUserRole.name()
                + ": "
                + currentUser.getFullName();
    }

    private void checkAndUpdateVenueReservationStatus(VenueReservation reservation) {
        if (reservation.getStatus() != Status.PENDING) {
            return;
        }

        List<VenueApproval> approvals =
                venueApprovalRepository.findAllByVenueReservationAndStatus(
                        reservation, Status.APPROVED);

        Set<Role> approvingRoles =
                approvals.stream().map(a -> a.getSignedBy().getRoles()).collect(Collectors.toSet());

        boolean allRequiredApproved = approvingRoles.containsAll(REQUIRED_APPROVAL_ROLES);

        if (allRequiredApproved) {
            reservation.setStatus(Status.APPROVED);
            venueReservationRepository.save(reservation);
            notifyRequester(
                    reservation, "fully approved", null, "VENUE_RESERVATION_FULLY_APPROVED");
        }
        // Add logic for other statuses if needed (e.g., PARTIALLY_APPROVED)
    }

    @Transactional
    public String cancelReservation(Long reservationId) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));

        // Authorization: Allow requester or SUPER_ADMIN to cancel
        boolean isRequester = reservation.getRequestingUser().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;

        if (!isRequester && !isSuperAdmin) {
            throw new SecurityException("You are not authorized to cancel this reservation.");
        }

        if (reservation.getStatus() == Status.CANCELED) {
            return "Warning: Reservation is already canceled.";
        }
        // Potentially restrict canceling already APPROVED reservations without specific
        // permission

        reservation.setStatus(Status.CANCELED);
        venueReservationRepository.save(reservation);

        notifyVenueOwnerOfCancellation(reservation, currentUser);
        notifyRequester(reservation, "cancelled", currentUser, "VENUE_RESERVATION_CANCELLED");

        return "Venue reservation canceled successfully.";
    }

    @Transactional
    public void deleteReservation(Long reservationId) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));

        // Authorization: Allow requester or SUPER_ADMIN to delete (maybe only if
        // CANCELED/REJECTED?)
        boolean isRequester = reservation.getRequestingUser().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;

        // Example: Only allow deletion if CANCELED or REJECTED, by requester or admin
        if (!isSuperAdmin
                && (!isRequester
                        || (reservation.getStatus() != Status.CANCELED
                                && reservation.getStatus() != Status.REJECTED))) {
            throw new SecurityException(
                    "You are not authorized to delete this reservation or it's not in a deletable"
                            + " state.");
        }

        venueReservationRepository.delete(reservation);
    }

    // --- Helper Methods ---

    private User getCurrentUser() {
        String username =
                ((UserDetails)
                                SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal())
                        .getUsername();
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private void notifyVenueOwner(VenueReservation reservation) {
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner != null && venueOwner.getEmail() != null) {
            String message =
                    String.format(
                            "New venue reservation request for '%s' (Event: %s) requires your"
                                    + " approval.",
                            reservation.getVenue().getName(),
                            reservation.getEvent().getEventName());

            // Create payload with entity info
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VENUE_RESERVATION_REQUEST");
            payload.put("message", message);
            payload.put("venueReservationId", reservation.getId());
            payload.put("relatedEntityType", "VENUE_RESERVATION");
            payload.put("eventId", reservation.getEvent().getId());
            payload.put("requesterName", reservation.getRequestingUser().getFullName());
            payload.put("eventName", reservation.getEvent().getEventName());
            payload.put("venueName", reservation.getVenue().getName());

            notificationService.notifyUser(venueOwner.getEmail(), "/queue/notifications", payload);
            System.out.println(
                    "DEBUG: Notify Venue Owner: "
                            + venueOwner.getEmail()
                            + " - Payload: "
                            + payload);
        }
    }

    private void notifyRequester(
            VenueReservation reservation, String action, User actor, String notificationType) {
        User requester = reservation.getRequestingUser();
        if (requester != null && requester.getEmail() != null) {
            String actorName = (actor != null) ? actor.getFullName() : "System";
            String message =
                    String.format(
                            "Your venue reservation for '%s' (Event: %s) has been %s%s.",
                            reservation.getVenue().getName(),
                            reservation.getEvent().getEventName(),
                            action,
                            (actor != null ? " by " + actorName : ""));

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", notificationType);
            payload.put("message", message);
            payload.put("venueReservationId", reservation.getId());
            payload.put("relatedEntityType", "VENUE_RESERVATION");
            payload.put("eventId", reservation.getEvent().getId());
            payload.put("status", reservation.getStatus().name());
            if (actor != null) {
                payload.put("actorName", actorName);
            }

            notificationService.notifyUser(requester.getEmail(), "/queue/notifications", payload);
            System.out.println(
                    "DEBUG: Notify Requester: " + requester.getEmail() + " - Payload: " + payload);
        }
    }

    private void notifyVenueOwnerOfCancellation(VenueReservation reservation, User canceller) {
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner != null
                && venueOwner.getEmail() != null
                && !venueOwner.getId().equals(canceller.getId())) {
            String message =
                    String.format(
                            "Venue reservation for '%s' (Event: %s) was cancelled by %s.",
                            reservation.getVenue().getName(),
                            reservation.getEvent().getEventName(),
                            canceller.getFullName());

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VENUE_RESERVATION_CANCELLED_INFO");
            payload.put("message", message);
            payload.put("venueReservationId", reservation.getId());
            payload.put("relatedEntityType", "VENUE_RESERVATION");
            payload.put("eventId", reservation.getEvent().getId());
            payload.put("cancellerName", canceller.getFullName());

            notificationService.notifyUser(venueOwner.getEmail(), "/queue/notifications", payload);
            System.out.println(
                    "DEBUG: Notify Owner of Cancellation: "
                            + venueOwner.getEmail()
                            + " - Payload: "
                            + payload);
        }
    }

    public UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
            try {
                profileImageUrl =
                        fileStorageService.getFileUrl(user.getProfileImagePath(), usersBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for user "
                                + user.getId()
                                + ": "
                                + e.getMessage());
            }
        }
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname() != null ? user.getFirstname() : null,
                user.getLastname() != null ? user.getLastname() : null,
                user.getId_number() != null ? user.getId_number() : null,
                user.getPhone_number() != null ? user.getPhone_number() : null,
                user.getTelephoneNumber() != null ? user.getTelephoneNumber() : null,
                user.getRoles() != null ? user.getRoles().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public VenueReservationDTO mapToDTO(VenueReservation reservation) {
        UserDTO requesterDto = mapUserToDTO(reservation.getRequestingUser());
        List<VenueApprovalDTO> approvalDTOs =
                reservation.getApprovals() != null
                        ? reservation.getApprovals().stream()
                                .map(this::mapApprovalToDTO)
                                .collect(Collectors.toList())
                        : List.of();

        return new VenueReservationDTO(
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getEvent().getEventName(),
                requesterDto,
                reservation.getDepartment().getId(),
                reservation.getDepartment().getName(),
                reservation.getVenue().getId(),
                reservation.getVenue().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                approvalDTOs,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    private VenueApprovalDTO mapApprovalToDTO(VenueApproval approval) {
        return new VenueApprovalDTO(
                approval.getId(),
                approval.getVenueReservation().getId(),
                approval.getSignedBy().getId(),
                approval.getSignedBy().getFullName(),
                approval.getSignedBy().getRoles().name(),
                approval.getRemarks(),
                approval.getStatus().name(),
                approval.getDateSigned());
    }

    public List<VenueApprovalDTO> getAllApprovalsForReservation(Long reservationId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with ID: "
                                                        + reservationId));
        return venueApprovalRepository.findAllByVenueReservation(reservation).stream()
                .map(this::mapApprovalToDTO)
                .collect(Collectors.toList());
    }

    public List<VenueReservationDTO> getPendingReservationsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return List.of();
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findPendingReservationsForVenueOwner(
                        currentUser.getId());
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<VenueReservationDTO> getAllReservationsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return List.of(); // Or throw exception
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findAllReservationsForVenueOwner(currentUser.getId());
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
