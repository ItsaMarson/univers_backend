/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentApprovalDTO;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.DTO.UserDTO;
// Import necessary entities
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.EquipmentApproval;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
// Import necessary repositories
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.EquipmentApprovalRepository;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.EquipmentReservationRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.LocalDateTime;
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
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentReservationService {

    private final EquipmentReservationRepository equipmentReservationRepository;
    private final EquipmentApprovalRepository equipmentApprovalRepository;
    private final EventRepository eventRepository;
    private final EquipmentRepository equipmentRepository; // Assuming this exists
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    // Define roles that can approve/reject equipment reservations
    // Assuming EQUIPMENT_OWNER is the primary role
    private static final Set<Role> EQUIPMENT_APPROVER_ROLES =
            Set.of(Role.EQUIPMENT_OWNER /*, Add other roles if needed */);
    // Define roles REQUIRED for the reservation to become APPROVED
    private static final Set<Role> REQUIRED_APPROVAL_ROLES = Set.of(Role.EQUIPMENT_OWNER);

    @Value("${minio.bucket.equipment-reservation-letters}")
    private String reservationLettersBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    // Constructor Injection
    public EquipmentReservationService(
            EquipmentReservationRepository equipmentReservationRepository,
            EquipmentApprovalRepository equipmentApprovalRepository,
            EventRepository eventRepository,
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            FileStorageService fileStorageService,
            NotificationService notificationService) {
        this.equipmentReservationRepository = equipmentReservationRepository;
        this.equipmentApprovalRepository = equipmentApprovalRepository;
        this.eventRepository = eventRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public EquipmentReservationDTO createEquipmentReservation(
            EquipmentReservationDTO reservationDTO, MultipartFile reservationLetterFile) {
        User requestingUser = getCurrentUser();

        Event event =
                eventRepository
                        .findById(reservationDTO.eventId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Associated Event not found with ID: "
                                                        + reservationDTO.eventId()));

        Equipment equipment =
                equipmentRepository
                        .findById(reservationDTO.equipmentId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: "
                                                        + reservationDTO.equipmentId()));

        Long departmentIdToFind;
        if (reservationDTO.departmentId() != null) {
            departmentIdToFind = reservationDTO.departmentId();
        } else {
            // Check if the requesting user has a department assigned
            Department userDepartment = requestingUser.getDepartment();
            if (userDepartment == null) {
                // Handle the case where the user has no department and none was provided in the DTO
                throw new IllegalArgumentException(
                        "Requesting user '"
                                + requestingUser.getEmail()
                                + "' does not have an assigned department, and no department ID was"
                                + " provided in the reservation request.");
            }
            departmentIdToFind = userDepartment.getId();
        }

        // Fetch the department using the determined ID
        Department department =
                departmentRepository
                        .findById(departmentIdToFind)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Department not found with ID: "
                                                        + departmentIdToFind));
        LocalDateTime startTime =
                reservationDTO.startTime() != null
                        ? reservationDTO.startTime()
                        : event.getStartTime();
        LocalDateTime endTime =
                reservationDTO.endTime() != null ? reservationDTO.endTime() : event.getEndTime();
        Integer requestedQuantity = reservationDTO.quantity();

        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive.");
        }

        // Check Availability (Simplified: checks total quantity reserved in the overlapping period)
        int currentlyReserved =
                equipmentReservationRepository
                        .findOverlappingReservations(equipment.getId(), startTime, endTime)
                        .stream()
                        .filter(
                                r ->
                                        r.getStatus() == Status.APPROVED
                                                || r.getStatus()
                                                        == Status.PENDING) // Consider pending as
                        // potentially unavailable
                        .mapToInt(EquipmentReservation::getQuantity)
                        .sum();

        if (equipment.getQuantity() < currentlyReserved + requestedQuantity) {
            throw new IllegalArgumentException(
                    String.format(
                            "Not enough equipment available. Requested: %d, Available during"
                                    + " period: %d",
                            requestedQuantity, equipment.getQuantity() - currentlyReserved));
        }

        EquipmentReservation newReservation = new EquipmentReservation();
        newReservation.setEvent(event);
        newReservation.setRequestingUser(requestingUser);
        newReservation.setDepartment(department);
        newReservation.setEquipment(equipment);
        newReservation.setQuantity(requestedQuantity);
        newReservation.setStartTime(startTime);
        newReservation.setEndTime(endTime);
        // Status set by @PrePersist

        if (reservationLetterFile != null && !reservationLetterFile.isEmpty()) {
            String letterObjectName =
                    fileStorageService.uploadFile(
                            reservationLetterFile,
                            reservationLettersBucketName,
                            "equipment-reservation-letters/");
            newReservation.setReservationLetterPath(letterObjectName);
        }

        EquipmentReservation savedReservation = equipmentReservationRepository.save(newReservation);

        notifyEquipmentOwner(savedReservation); // Notify the equipment owner

        return mapToDTO(savedReservation);
    }

    public List<EquipmentReservationDTO> getAllReservations() {
        return equipmentReservationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public EquipmentReservationDTO getReservationById(Long reservationId) {
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with ID: "
                                                        + reservationId));
        return mapToDTO(reservation);
    }

    public List<EquipmentReservationDTO> getOwnEquipmentReservations() {
        User currentUser = getCurrentUser();
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findByRequestingUser(currentUser);
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public String approveReservation(Long reservationId, String remarks) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found: "
                                                        + reservationId));

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not PENDING.";
        }

        Role currentUserRole = currentUser.getRoles();
        if (!EQUIPMENT_APPROVER_ROLES.contains(currentUserRole)) {
            return "Error: You lack the required role to approve.";
        }

        // Specific check for EQUIPMENT_OWNER
        if (currentUserRole == Role.EQUIPMENT_OWNER) {
            User equipmentOwner = reservation.getEquipment().getEquipmentOwner();
            if (equipmentOwner == null || !currentUser.getId().equals(equipmentOwner.getId())) {
                return "Error: You are not the designated owner for this equipment.";
            }
        }
        // Add checks for other roles if needed

        if (equipmentApprovalRepository.existsByEquipmentReservationAndSignedByAndStatus(
                reservation, currentUser, Status.APPROVED)) {
            return "Warning: You have already approved this.";
        }

        EquipmentApproval approval = new EquipmentApproval();
        approval.setEquipmentReservation(reservation);
        approval.setSignedBy(currentUser);
        approval.setStatus(Status.APPROVED);
        approval.setRemarks(remarks);
        equipmentApprovalRepository.save(approval);

        checkAndUpdateEquipmentReservationStatus(reservation);

        notifyRequester(
                reservation, "received approval from " + currentUserRole.name(), currentUser);

        return "Equipment reservation approved by "
                + currentUserRole.name()
                + ": "
                + currentUser.getFullName();
    }

    @Transactional
    public String rejectReservation(Long reservationId, String remarks) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found: "
                                                        + reservationId));

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not PENDING.";
        }

        Role currentUserRole = currentUser.getRoles();
        if (!EQUIPMENT_APPROVER_ROLES.contains(currentUserRole)) {
            return "Error: You lack the required role to reject.";
        }

        // Specific check for EQUIPMENT_OWNER
        if (currentUserRole == Role.EQUIPMENT_OWNER) {
            User equipmentOwner = reservation.getEquipment().getEquipmentOwner();
            if (equipmentOwner == null || !currentUser.getId().equals(equipmentOwner.getId())) {
                return "Error: You are not the designated owner for this equipment.";
            }
        }
        // Add checks for other roles if needed

        EquipmentApproval rejectionRecord = new EquipmentApproval();
        rejectionRecord.setEquipmentReservation(reservation);
        rejectionRecord.setSignedBy(currentUser);
        rejectionRecord.setStatus(Status.REJECTED);
        rejectionRecord.setRemarks(remarks);
        equipmentApprovalRepository.save(rejectionRecord);

        reservation.setStatus(Status.REJECTED);
        equipmentReservationRepository.save(reservation);

        notifyRequester(reservation, "rejected", currentUser);

        return "Equipment reservation rejected by "
                + currentUserRole.name()
                + ": "
                + currentUser.getFullName();
    }

    private void checkAndUpdateEquipmentReservationStatus(EquipmentReservation reservation) {
        if (reservation.getStatus() != Status.PENDING) {
            return;
        }

        List<EquipmentApproval> approvals =
                equipmentApprovalRepository.findAllByEquipmentReservationAndStatus(
                        reservation, Status.APPROVED);
        Set<Role> approvingRoles =
                approvals.stream().map(a -> a.getSignedBy().getRoles()).collect(Collectors.toSet());

        boolean allRequiredApproved = approvingRoles.containsAll(REQUIRED_APPROVAL_ROLES);

        if (allRequiredApproved) {
            reservation.setStatus(Status.APPROVED);
            equipmentReservationRepository.save(reservation);
            notifyRequester(reservation, "fully approved", null);
        }
    }

    @Transactional
    public String cancelReservation(Long reservationId) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found: "
                                                        + reservationId));

        boolean isRequester = reservation.getRequestingUser().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;

        if (!isRequester && !isSuperAdmin) {
            throw new SecurityException("You are not authorized to cancel this reservation.");
        }

        if (reservation.getStatus() == Status.CANCELED) {
            return "Warning: Reservation is already canceled.";
        }

        reservation.setStatus(Status.CANCELED);
        equipmentReservationRepository.save(reservation);
        // Notify owner?
        return "Equipment reservation canceled successfully.";
    }

    @Transactional
    public void deleteReservation(Long reservationId) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found: "
                                                        + reservationId));

        boolean isRequester = reservation.getRequestingUser().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;

        if (!isSuperAdmin
                && (!isRequester
                        || (reservation.getStatus() != Status.CANCELED
                                && reservation.getStatus() != Status.REJECTED))) {
            throw new SecurityException("Cannot delete this reservation.");
        }

        if (reservation.getReservationLetterPath() != null
                && !reservation.getReservationLetterPath().isBlank()) {
            fileStorageService.deleteFile(
                    reservation.getReservationLetterPath(), reservationLettersBucketName);
        }

        equipmentReservationRepository.delete(reservation);
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

    private void notifyEquipmentOwner(EquipmentReservation reservation) {
        User equipmentOwner = reservation.getEquipment().getEquipmentOwner();
        if (equipmentOwner != null && equipmentOwner.getEmail() != null) {
            String message =
                    String.format(
                            "New equipment reservation request for '%s' (Qty: %d, Event: %s)"
                                    + " requires your approval.",
                            reservation.getEquipment().getName(),
                            reservation.getQuantity(),
                            reservation.getEvent().getEventName());
            notificationService.notifyUser(
                    equipmentOwner.getEmail(), "/queue/notifications", Map.of("message", message));
            System.out.println(
                    "DEBUG: Notify Equipment Owner: "
                            + equipmentOwner.getEmail()
                            + " - "
                            + message);
        }
    }

    private void notifyRequester(EquipmentReservation reservation, String action, User actor) {
        User requester = reservation.getRequestingUser();
        if (requester != null && requester.getEmail() != null) {
            String actorName = (actor != null) ? actor.getFullName() : "System";
            String message =
                    String.format(
                            "Your equipment reservation for '%s' (Qty: %d, Event: %s) has been"
                                    + " %s%s.",
                            reservation.getEquipment().getName(),
                            reservation.getQuantity(),
                            reservation.getEvent().getEventName(),
                            action,
                            (actor != null ? " by " + actorName : ""));
            notificationService.notifyUser(
                    requester.getEmail(), "/queue/notifications", Map.of("message", message));
            System.out.println(
                    "DEBUG: Notify Requester: " + requester.getEmail() + " - " + message);
        }
    }

    // --- Mappers ---
    public EquipmentReservationDTO mapToDTO(EquipmentReservation reservation) {
        String letterUrl = null;
        if (reservation.getReservationLetterPath() != null
                && !reservation.getReservationLetterPath().isBlank()) {
            letterUrl =
                    fileStorageService.getFileUrl(
                            reservation.getReservationLetterPath(), reservationLettersBucketName);
        }
        UserDTO requesterDto = mapUserToDTO(reservation.getRequestingUser());
        List<EquipmentApprovalDTO> approvalDTOs =
                reservation.getApprovals() != null
                        ? reservation.getApprovals().stream()
                                .map(this::mapApprovalToDTO)
                                .collect(Collectors.toList())
                        : List.of();

        return new EquipmentReservationDTO(
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getEvent().getEventName(),
                requesterDto,
                reservation.getDepartment().getId(),
                reservation.getDepartment().getName(),
                reservation.getEquipment().getId(),
                reservation.getEquipment().getName(),
                reservation.getQuantity(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                letterUrl,
                approvalDTOs,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    private EquipmentApprovalDTO mapApprovalToDTO(EquipmentApproval approval) {
        return new EquipmentApprovalDTO(
                approval.getId(),
                approval.getEquipmentReservation().getId(),
                approval.getSignedBy().getId(),
                approval.getSignedBy().getFullName(),
                approval.getSignedBy().getRoles().name(),
                approval.getRemarks(),
                approval.getStatus().name(),
                approval.getDateSigned());
    }

    // Reuse UserDTO mapping logic (adapt if needed)
    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
            try {
                profileImageUrl =
                        fileStorageService.getFileUrl(user.getProfileImagePath(), usersBucketName);
            } catch (Exception e) {
                /* Handle error */
            }
        }
        // Adapt fields based on your UserDTO structure
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles() != null ? user.getRoles().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // --- Getters for specific lists ---
    public List<EquipmentApprovalDTO> getAllApprovalsForReservation(Long reservationId) {
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found: "
                                                        + reservationId));
        return equipmentApprovalRepository.findAllByEquipmentReservation(reservation).stream()
                .map(this::mapApprovalToDTO)
                .collect(Collectors.toList());
    }

    public List<EquipmentReservationDTO> getPendingReservationsForEquipmentOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.EQUIPMENT_OWNER.toString())) {
            return List.of();
        }
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findPendingReservationsForEquipmentOwner(
                        currentUser.getId());
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<EquipmentReservationDTO> getAllReservationsForEquipmentOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.EQUIPMENT_OWNER.toString())) {
            return List.of();
        }
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findAllReservationsForEquipmentOwner(
                        currentUser.getId());
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
