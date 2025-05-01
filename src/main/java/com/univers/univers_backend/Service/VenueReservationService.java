/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueApprovalDTO;
import com.univers.univers_backend.DTO.VenueReservationDTO;
// Import necessary entities
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Entity.VenueApproval;
import com.univers.univers_backend.Entity.VenueReservation;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
// Import necessary repositories
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueApprovalRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.Repository.VenueReservationRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VenueReservationService {

    private final VenueReservationRepository venueReservationRepository;
    private final VenueApprovalRepository venueApprovalRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository; // Assuming you have this
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Value("${minio.bucket.venuesreservationletters}")
    private String reservationLettersBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    // Constructor Injection

    public VenueReservationService(
            VenueReservationRepository venueReservationRepository,
            VenueApprovalRepository venueApprovalRepository,
            EventRepository eventRepository,
            VenueRepository venueRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            FileStorageService fileStorageService,
            NotificationService notificationService /*... other dependencies */) {
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
    public VenueReservationDTO createVenueReservation(
            VenueReservationDTO reservationDTO, MultipartFile reservationLetterFile) {
        // 1. Get current user
        User requestingUser = getCurrentUser();

        // 2. Validate Event
        Event event =
                eventRepository
                        .findById(reservationDTO.eventId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Associated Event not found with ID: "
                                                        + reservationDTO.eventId()));
        // Optional: Check if event status is suitable (e.g., not CANCELED)

        // 3. Validate Venue
        Venue venue =
                venueRepository
                        .findById(reservationDTO.venueId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with ID: "
                                                        + reservationDTO.venueId()));

        // 4. Validate Department (Assuming department comes from user or DTO)
        //    If departmentId is not in DTO, get from requestingUser
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

        // 5. Check for Time Conflicts
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
        // Optional: Also check Event conflicts for the same venue/time if not implicitly handled

        // 6. Create VenueReservation Entity
        VenueReservation newReservation = new VenueReservation();
        newReservation.setEvent(event);
        newReservation.setRequestingUser(requestingUser);
        newReservation.setDepartment(department);
        newReservation.setVenue(venue);
        newReservation.setStartTime(startTime);
        newReservation.setEndTime(endTime);
        // Status is set to PENDING by @PrePersist

        // 7. Handle File Upload
        if (reservationLetterFile != null && !reservationLetterFile.isEmpty()) {
            String letterObjectName =
                    fileStorageService.uploadFile(
                            reservationLetterFile,
                            reservationLettersBucketName,
                            "reservation-letters/");
            newReservation.setReservationLetterPath(letterObjectName);
        }

        // 8. Save Entity
        VenueReservation savedReservation = venueReservationRepository.save(newReservation);

        // 9. Notify Approvers (e.g., Venue Owner)
        notifyVenueOwner(savedReservation);

        // 10. Map to DTO and return
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

    // --- Approval Logic ---

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

        // Check if reservation is in a state that can be approved (e.g., PENDING)
        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not in PENDING state.";
        }

        // Determine required approver (e.g., Venue Owner)
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner == null) {
            return "Error: Venue Owner not assigned to the venue.";
        }

        // Check if current user is the required approver
        if (!currentUser.getId().equals(venueOwner.getId())) {
            // Add checks for other potential approver roles if needed
            return "Error: You are not authorized to approve this reservation (Requires Venue"
                    + " Owner).";
        }
        // Check if user has the correct role (e.g., VENUE_OWNER)
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return "Error: User does not have the required VENUE_OWNER role.";
        }

        // Check if already approved by this user
        if (venueApprovalRepository.existsByVenueReservationAndSignedByAndStatus(
                reservation, currentUser, Status.APPROVED)) {
            return "Warning: You have already approved this reservation.";
        }

        // Create VenueApproval record
        VenueApproval approval = new VenueApproval();
        approval.setVenueReservation(reservation);
        approval.setSignedBy(currentUser);
        approval.setStatus(Status.APPROVED); // Or REJECTED based on another method/parameter
        approval.setRemarks(remarks);
        // dateSigned is set by @PrePersist

        venueApprovalRepository.save(approval);

        // Check if all required approvals are met and update reservation status
        checkAndUpdateVenueReservationStatus(reservation);

        // Notify requester
        notifyRequester(reservation, "approved", currentUser);

        return "Venue reservation approved successfully by " + currentUser.getFullName();
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

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not in PENDING state.";
        }

        // Similar authorization checks as approveReservation
        User venueOwner = reservation.getVenue().getVenueOwner();
        if (venueOwner == null || !currentUser.getId().equals(venueOwner.getId())) {
            // Add checks for other potential approver roles if needed
            return "Error: You are not authorized to reject this reservation.";
        }
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return "Error: User does not have the required VENUE_OWNER role.";
        }

        // Create VenueApproval record
        VenueApproval approval = new VenueApproval();
        approval.setVenueReservation(reservation);
        approval.setSignedBy(currentUser);
        approval.setStatus(Status.REJECTED); // Set status to REJECTED
        approval.setRemarks(remarks);
        venueApprovalRepository.save(approval);

        // Update reservation status directly to REJECTED
        reservation.setStatus(Status.REJECTED);
        venueReservationRepository.save(reservation);

        // Notify requester
        notifyRequester(reservation, "rejected", currentUser);

        return "Venue reservation rejected successfully by " + currentUser.getFullName();
    }

    private void checkAndUpdateVenueReservationStatus(VenueReservation reservation) {
        // Skip if already decided (Approved, Rejected, Canceled)
        if (reservation.getStatus() != Status.PENDING) {
            return;
        }

        List<VenueApproval> approvals =
                venueApprovalRepository.findAllByVenueReservationAndStatus(
                        reservation, Status.APPROVED);

        // Define required approvals (e.g., just Venue Owner for now)
        boolean hasVenueOwnerApproval =
                approvals.stream()
                        .anyMatch(
                                a ->
                                        a.getSignedBy()
                                                .getId()
                                                .equals(
                                                        reservation
                                                                .getVenue()
                                                                .getVenueOwner()
                                                                .getId()));
        // Add more checks if other roles need to approve (e.g., Department Head, Admin)
        // boolean hasDeptHeadApproval = ...;

        if (hasVenueOwnerApproval /* && hasDeptHeadApproval etc. */) {
            reservation.setStatus(Status.APPROVED);
            venueReservationRepository.save(reservation);
            // Notify requester about final approval
            notifyRequester(reservation, "fully approved", null); // Or pass the last approver
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
        // Potentially restrict canceling already APPROVED reservations without specific permission

        reservation.setStatus(Status.CANCELED);
        venueReservationRepository.save(reservation);

        // Notify relevant parties (e.g., venue owner if it was pending/approved)
        // notifyVenueOwnerOfCancellation(reservation);

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

        // Delete associated letter file
        if (reservation.getReservationLetterPath() != null
                && !reservation.getReservationLetterPath().isBlank()) {
            fileStorageService.deleteFile(
                    reservation.getReservationLetterPath(), reservationLettersBucketName);
        }

        // Approvals are deleted via cascade

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
            // Use notificationService similar to EventService/EventApprovalService
            // notificationService.notifyUser(venueOwner.getEmail(), "/queue/notifications",
            // Map.of(...));
            System.out.println(
                    "DEBUG: Notify Venue Owner: " + venueOwner.getEmail() + " - " + message);
        }
    }

    private void notifyRequester(VenueReservation reservation, String action, User actor) {
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
            // Use notificationService
            // notificationService.notifyUser(requester.getEmail(), "/queue/notifications",
            // Map.of(...));
            System.out.println(
                    "DEBUG: Notify Requester: " + requester.getEmail() + " - " + message);
        }
    }

    // --- Mappers ---
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
        String letterUrl = null;
        if (reservation.getReservationLetterPath() != null
                && !reservation.getReservationLetterPath().isBlank()) {
            letterUrl =
                    fileStorageService.getFileUrl(
                            reservation.getReservationLetterPath(), reservationLettersBucketName);
        }

        UserDTO requesterDto =
                mapUserToDTO(reservation.getRequestingUser()); // Reuse existing mapper if available
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
                letterUrl,
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
                approval.getSignedBy().getRoles().name(), // Assuming single role
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

    // Method to get pending reservations for the current venue owner
    public List<VenueReservationDTO> getPendingReservationsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return List.of(); // Or throw exception
        }
        List<VenueReservation> reservations =
                venueReservationRepository.findPendingReservationsForVenueOwner(
                        currentUser.getId());
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
