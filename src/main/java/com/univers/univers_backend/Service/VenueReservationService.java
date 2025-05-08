/* (C)2025 */
package com.univers.univers_backend.Service;

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
import com.univers.univers_backend.Mapper.*;
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
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueReservationService {

    private static final Logger logger = LoggerFactory.getLogger(VenueReservationService.class);

    private final VenueReservationRepository venueReservationRepository;
    private final VenueApprovalRepository venueApprovalRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    private final VenueReservationMapper venueReservationMapper;
    private final VenueApprovalMapper venueApprovalMapper;
    private final UserMapper userMapper;

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
            NotificationService notificationService,
            @Lazy VenueReservationMapper venueReservationMapper,
            @Lazy VenueApprovalMapper venueApprovalMapper,
            @Lazy UserMapper userMapper) {
        this.venueReservationRepository = venueReservationRepository;
        this.venueApprovalRepository = venueApprovalRepository;
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.venueReservationMapper = venueReservationMapper;
        this.venueApprovalMapper = venueApprovalMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public VenueReservationDTO createVenueReservation(VenueReservationDTO reservationDTO) {
        User requestingUser = getCurrentUser();

        if (reservationDTO.event() == null || reservationDTO.event().publicId() == null) {
            throw new IllegalArgumentException(
                    "Event with publicId is required in reservation DTO.");
        }
        Event event =
                eventRepository
                        .findByPublicId(reservationDTO.event().publicId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Associated Event not found with Public ID: "
                                                        + reservationDTO.event().publicId()));

        if (reservationDTO.venue() == null || reservationDTO.venue().publicId() == null) {
            throw new IllegalArgumentException(
                    "Venue with publicId is required in reservation DTO.");
        }
        Venue venue =
                venueRepository
                        .findByPublicId(reservationDTO.venue().publicId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with Public ID: "
                                                        + reservationDTO.venue().publicId()));

        Department department = null;
        if (reservationDTO.department() != null && reservationDTO.department().publicId() != null) {
            department =
                    departmentRepository
                            .findByPublicId(reservationDTO.department().publicId())
                            .orElseThrow(
                                    () ->
                                            new NoSuchElementException(
                                                    "Department not found with Public ID: "
                                                            + reservationDTO
                                                                    .department()
                                                                    .publicId()));
        } else if (requestingUser.getDepartment() != null) {
            department = requestingUser.getDepartment();
        }

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
        newReservation.setStatus(Status.PENDING);

        VenueReservation savedReservation = venueReservationRepository.save(newReservation);

        notifyVenueOwner(savedReservation);

        return venueReservationMapper.toDto(savedReservation);
    }

    public List<VenueReservationDTO> getAllReservations() {
        return venueReservationRepository.findAll().stream()
                .map(venueReservationMapper::toDto)
                .collect(Collectors.toList());
    }

    public VenueReservationDTO getReservationByPublicId(UUID publicId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + publicId));
        return venueReservationMapper.toDto(reservation);
    }

    public List<VenueReservationDTO> getOwnVenueReservations() {
        User currentUser = getCurrentUser();
        List<VenueReservation> reservations =
                venueReservationRepository.findByRequestingUser(currentUser);
        return reservations.stream()
                .map(venueReservationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public String approveReservation(UUID reservationPublicId, String remarks) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + reservationPublicId));

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
            if (!currentUser.getPublicId().equals(venueOwner.getPublicId())) {
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
    public String rejectReservation(UUID reservationPublicId, String remarks) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + reservationPublicId));

        if (reservation.getStatus() != Status.PENDING
                && reservation.getStatus() != Status.APPROVED) {
            return "Error: Reservation cannot be rejected as it is currently "
                    + reservation.getStatus();
        }

        Role currentUserRole = currentUser.getRoles();
        if (!VENUE_APPROVER_ROLES.contains(currentUserRole)) {
            return "Error: You do not have the required role to reject this venue reservation.";
        }

        if (currentUserRole == Role.VENUE_OWNER) {
            User venueOwner = reservation.getVenue().getVenueOwner();
            if (venueOwner == null) {
                return "Error: Venue Owner not assigned to the venue.";
            }
            if (!currentUser.getPublicId().equals(venueOwner.getPublicId())) {
                return "Error: You are not the designated Venue Owner for this venue.";
            }
        }

        if (venueApprovalRepository.existsByVenueReservationAndSignedByAndStatus(
                reservation, currentUser, Status.REJECTED)) {
            return "Warning: You have already rejected this reservation.";
        }

        VenueApproval rejection = new VenueApproval();
        rejection.setVenueReservation(reservation);
        rejection.setSignedBy(currentUser);
        rejection.setStatus(Status.REJECTED);
        rejection.setRemarks(remarks);

        venueApprovalRepository.save(rejection);

        reservation.setStatus(Status.REJECTED);
        venueReservationRepository.save(reservation);

        notifyRequester(
                reservation,
                "rejected by " + currentUserRole.name(),
                currentUser,
                "VENUE_RESERVATION_REJECTED");

        return "Venue reservation rejected by "
                + currentUserRole.name()
                + ": "
                + currentUser.getFullName();
    }

    private void checkAndUpdateVenueReservationStatus(VenueReservation reservation) {
        if (reservation.getStatus() != Status.PENDING) {
            return;
        }

        long requiredApprovalsCount = REQUIRED_APPROVAL_ROLES.size();
        long currentApprovalsCount =
                venueApprovalRepository
                        .findAllByVenueReservationAndStatus(reservation, Status.APPROVED)
                        .stream()
                        .map(approval -> approval.getSignedBy().getRoles())
                        .distinct()
                        .count();
        if (currentApprovalsCount >= requiredApprovalsCount) {
            reservation.setStatus(Status.APPROVED);
            venueReservationRepository.save(reservation);
            notifyRequester(
                    reservation, "fully approved", null, "VENUE_RESERVATION_FULLY_APPROVED");
        }
    }

    @Transactional
    public String cancelReservation(UUID reservationPublicId) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + reservationPublicId));

        boolean isAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;
        boolean isRequester =
                reservation.getRequestingUser().getPublicId().equals(currentUser.getPublicId());

        if (!isAdmin && !isRequester) {
            throw new SecurityException("You are not authorized to cancel this reservation.");
        }
        if (reservation.getStatus() == Status.CANCELED) {
            return "Reservation is already canceled.";
        }
        if (reservation.getStatus() == Status.COMPLETED
                || reservation.getStatus() == Status.ONGOING) {
            throw new IllegalStateException(
                    "Cannot cancel a reservation that is ongoing or already completed.");
        }

        reservation.setStatus(Status.CANCELED);
        venueReservationRepository.save(reservation);
        notifyVenueOwnerOfCancellation(reservation, currentUser);
        return "Venue reservation canceled successfully.";
    }

    @Transactional
    public void deleteReservation(UUID reservationPublicId) {
        User currentUser = getCurrentUser();
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + reservationPublicId));

        venueApprovalRepository.deleteAll(reservation.getApprovals());
        venueReservationRepository.delete(reservation);
    }

    @Transactional
    public void deleteReservationsByEventPublicId(UUID eventPublicId) {
        // Find all venue reservations linked to this eventPublicId
        List<VenueReservation> reservations =
                venueReservationRepository.findByEvent_PublicId(eventPublicId);
        if (!reservations.isEmpty()) {
            // If reservations are found, delete them
            venueReservationRepository.deleteAll(reservations);
            logger.info(
                    "Deleted {} venue reservations for event public ID: {}",
                    reservations.size(),
                    eventPublicId);
        } else {
            logger.info(
                    "No venue reservations found for event public ID: {} to delete.",
                    eventPublicId);
        }
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository
                .findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private void notifyVenueOwner(VenueReservation reservation) {
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner != null && venueOwner.getEmail() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VENUE_RESERVATION_REQUEST");
            payload.put(
                    "message",
                    "New venue reservation request for your venue: "
                            + reservation.getVenue().getName());
            payload.put("reservationId", reservation.getPublicId());
            payload.put("eventId", reservation.getEvent().getPublicId());
            payload.put("eventName", reservation.getEvent().getEventName());
            payload.put("requesterName", reservation.getRequestingUser().getFullName());
            notificationService.createNotification(
                    venueOwner,
                    payload.get("message").toString(),
                    reservation.getEvent().getPublicId(),
                    reservation.getPublicId(),
                    "VENUE_RESERVATION_REQUEST");
        }
    }

    private void notifyRequester(
            VenueReservation reservation, String action, User actor, String notificationType) {
        User requester = reservation.getRequestingUser();
        if (requester != null && requester.getEmail() != null) {
            String actorName = (actor != null) ? actor.getFullName() : "System";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", notificationType);
            payload.put(
                    "message",
                    "Your venue reservation for '"
                            + reservation.getEvent().getEventName()
                            + "' has been "
                            + action
                            + " by "
                            + actorName
                            + ".");
            payload.put("reservationId", reservation.getPublicId());
            payload.put("eventId", reservation.getEvent().getPublicId());
            notificationService.createNotification(
                    requester,
                    payload.get("message").toString(),
                    reservation.getEvent().getPublicId(),
                    reservation.getPublicId(),
                    notificationType);
        }
    }

    private void notifyVenueOwnerOfCancellation(VenueReservation reservation, User canceller) {
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner != null && venueOwner.getEmail() != null) {
            String cancellerName = (canceller != null) ? canceller.getFullName() : "System";
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "VENUE_RESERVATION_CANCELED_INFO");
            payload.put(
                    "message",
                    "Venue reservation for event '"
                            + reservation.getEvent().getEventName()
                            + "' at your venue '"
                            + reservation.getVenue().getName()
                            + "' has been canceled by "
                            + cancellerName
                            + ".");
            payload.put("reservationId", reservation.getPublicId());
            payload.put("eventId", reservation.getEvent().getPublicId());
            notificationService.createNotification(
                    venueOwner,
                    payload.get("message").toString(),
                    reservation.getEvent().getPublicId(),
                    reservation.getPublicId(),
                    "VENUE_RESERVATION_CANCELED_INFO");
        }
    }

    public List<VenueApprovalDTO> getAllApprovalsForReservation(UUID reservationPublicId) {
        VenueReservation reservation =
                venueReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue Reservation not found with Public ID: "
                                                        + reservationPublicId));
        List<VenueApproval> approvals =
                venueApprovalRepository.findAllByVenueReservation(reservation);
        return approvals.stream().map(venueApprovalMapper::toDto).collect(Collectors.toList());
    }

    public List<VenueReservationDTO> getPendingReservationsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return List.of();
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findPendingReservationsForVenueOwner(
                        currentUser.getId());
        return reservations.stream()
                .map(venueReservationMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<VenueReservationDTO> getAllReservationsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return List.of();
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findAllReservationsForVenueOwner(currentUser.getId());
        return reservations.stream()
                .map(venueReservationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelReservationsForEvent(UUID eventPublicId) {
        List<VenueReservation> reservations =
                venueReservationRepository.findAllByEvent_PublicId(eventPublicId);
        User systemUser = null;
        try {
            systemUser = userRepository.findByEmail("admin@univers.com").orElse(null);
        } catch (Exception e) {
            System.err.println(
                    "Admin user for system actions not found, proceeding without actor for"
                            + " cancellation notification.");
        }

        for (VenueReservation reservation : reservations) {
            if (reservation.getStatus() != Status.CANCELED) {
                reservation.setStatus(Status.CANCELED);
                venueReservationRepository.save(reservation);
                notifyVenueOwnerOfCancellation(reservation, systemUser);
                notifyRequester(
                        reservation,
                        "canceled due to event cancellation",
                        systemUser,
                        "VENUE_RESERVATION_CANCELED_BY_EVENT");
            }
        }
    }
}
