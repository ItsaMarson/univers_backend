/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.EquipmentApprovalDTO;
import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.EquipmentReservationDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.EquipmentApproval;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.DepartmentMapper;
import com.univers.univers_backend.Mapper.EquipmentMapper;
import com.univers.univers_backend.Mapper.EventMapper;
import com.univers.univers_backend.Repository.DepartmentRepository;
import com.univers.univers_backend.Repository.EquipmentApprovalRepository;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.EquipmentReservationRepository;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentReservationService {

    private final EquipmentReservationRepository equipmentReservationRepository;
    private final EquipmentApprovalRepository equipmentApprovalRepository;
    private final EventRepository eventRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final EventApprovalRepository eventApprovalRepository;
    // Mappers
    private final EventMapper eventMapper;
    private final DepartmentMapper departmentMapper;
    private final EquipmentMapper equipmentMapper;
    private static final Logger logger = LoggerFactory.getLogger(EquipmentReservationService.class);
    // Define roles that can approve/reject equipment reservations
    // Assuming EQUIPMENT_OWNER is the primary role
    private static final Set<Role> EQUIPMENT_APPROVER_ROLES = Set.of(Role.EQUIPMENT_OWNER /*
                                                                                               * , Add other roles if
                                                                                               * needed
                                                                                               */);
    // Define roles REQUIRED for the reservation to become APPROVED
    private static final Set<Role> REQUIRED_APPROVAL_ROLES = Set.of(Role.EQUIPMENT_OWNER);

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
            NotificationService notificationService,
            EventApprovalRepository eventApprovalRepository,
            @Lazy EventMapper eventMapper,
            @Lazy DepartmentMapper departmentMapper,
            @Lazy EquipmentMapper equipmentMapper) {
        this.equipmentReservationRepository = equipmentReservationRepository;
        this.equipmentApprovalRepository = equipmentApprovalRepository;
        this.eventRepository = eventRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.eventApprovalRepository = eventApprovalRepository;
        this.eventMapper = eventMapper;
        this.departmentMapper = departmentMapper;
        this.equipmentMapper = equipmentMapper;
    }

    private EquipmentReservationDTO processSingleReservationCreation(
            EquipmentReservationDTO reservationDTO, User requestingUser) {
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

        if (reservationDTO.equipment() == null || reservationDTO.equipment().publicId() == null) {
            throw new IllegalArgumentException(
                    "Equipment with publicId is required in reservation DTO.");
        }
        Equipment equipment =
                equipmentRepository
                        .findByPublicId(reservationDTO.equipment().publicId())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with Public ID: "
                                                        + reservationDTO.equipment().publicId()));

        Department department;
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
        } else {
            Department userDepartment = requestingUser.getDepartment();
            if (userDepartment == null) {
                throw new IllegalArgumentException(
                        "Requesting user '"
                                + requestingUser.getEmail()
                                + "' does not have an assigned department, and no department was"
                                + " provided in the reservation request.");
            }
            department = userDepartment;
        }

        Instant startTime =
                reservationDTO.startTime() != null
                        ? reservationDTO.startTime()
                        : event.getStartTime();
        Instant endTime =
                reservationDTO.endTime() != null ? reservationDTO.endTime() : event.getEndTime();
        Integer requestedQuantity = reservationDTO.quantity();

        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive.");
        }

        // Check Availability using time-based overlapping reservations
        int currentlyReservedInPeriod =
                equipmentReservationRepository
                        .findOverlappingReservations(equipment.getId(), startTime, endTime)
                        .stream()
                        .filter(
                                r ->
                                        r.getStatus() == Status.APPROVED
                                                || r.getStatus() == Status.PENDING)
                        .mapToInt(EquipmentReservation::getQuantity)
                        .sum();

        if (equipment.getAvailableQuantity() < requestedQuantity) {
            throw new IllegalArgumentException(
                    String.format(
                            "Not enough %s available in inventory. Requested: %d, Available: %d",
                            equipment.getName(),
                            requestedQuantity,
                            equipment.getAvailableQuantity()));
        }

        if (equipment.getTotalQuantity() < currentlyReservedInPeriod + requestedQuantity) {
            throw new IllegalArgumentException(
                    String.format(
                            "Not enough %s available during the requested time period. Requested:"
                                + " %d, Available during period: %d, Currently Reserved in period:"
                                + " %d",
                            equipment.getName(),
                            requestedQuantity,
                            equipment.getTotalQuantity() - currentlyReservedInPeriod,
                            currentlyReservedInPeriod));
        }

        EquipmentReservation newReservation = new EquipmentReservation();
        newReservation.setEvent(event);
        newReservation.setRequestingUser(requestingUser);
        newReservation.setDepartment(department);
        newReservation.setEquipment(equipment);
        newReservation.setQuantity(requestedQuantity);
        newReservation.setStartTime(startTime);
        newReservation.setEndTime(endTime);
        // Status is PENDING by default as per Entity definition

        // Decrease available quantity immediately when reservation is created (PENDING status)
        Integer currentAvailable = equipment.getAvailableQuantity();
        equipment.setAvailableQuantity(currentAvailable - requestedQuantity);
        equipmentRepository.save(equipment);

        EquipmentReservation savedReservation = equipmentReservationRepository.save(newReservation);

        // Add equipment owner to event approvals if they don't already have one
        User equipmentOwner = equipment.getEquipmentOwner();
        if (equipmentOwner != null) {
            boolean hasExistingApproval =
                    eventApprovalRepository
                            .findByEventAndSignedBy(event, equipmentOwner)
                            .isPresent();
            if (!hasExistingApproval) {
                EventApproval equipmentOwnerApproval = new EventApproval();
                equipmentOwnerApproval.setEvent(event);
                equipmentOwnerApproval.setSignedBy(equipmentOwner);
                equipmentOwnerApproval.setStatus(Status.PENDING);
                eventApprovalRepository.save(equipmentOwnerApproval);

                // Notify equipment owner about the event approval requirement
                if (!equipmentOwner.getPublicId().equals(requestingUser.getPublicId())) {
                    notificationService.createNotification(
                            equipmentOwner,
                            "A new event '"
                                    + event.getEventName()
                                    + "' has requested your equipment '"
                                    + equipment.getName()
                                    + "' and requires your approval.",
                            event.getPublicId(),
                            event.getPublicId(),
                            "EVENT_EQUIPMENT_APPROVAL");
                }
            }
        }

        notifyEquipmentOwner(savedReservation);

        return mapToDTO(savedReservation);
    }

    @Transactional
    public EquipmentReservationDTO createEquipmentReservation(
            EquipmentReservationDTO reservationDTO) {
        User requestingUser = getCurrentUser();
        return processSingleReservationCreation(reservationDTO, requestingUser);
    }

    @Transactional
    public List<EquipmentReservationDTO> createBulkEquipmentReservations(
            List<EquipmentReservationDTO> reservationDTOs) {
        User requestingUser = getCurrentUser();
        List<EquipmentReservationDTO> createdReservations = new ArrayList<>();
        for (EquipmentReservationDTO reservationDTO : reservationDTOs) {
            // Consider adding try-catch here if one failure shouldn't roll back all,
            // but @Transactional on the method means all succeed or all fail.
            createdReservations.add(
                    processSingleReservationCreation(reservationDTO, requestingUser));
        }
        return createdReservations;
    }

    public List<EquipmentReservationDTO> getAllReservations() {
        return equipmentReservationRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public EquipmentReservationDTO getReservationByPublicId(UUID reservationPublicId) {
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));
        return mapToDTO(reservation);
    }

    public List<EquipmentReservationDTO> getOwnEquipmentReservations() {
        User currentUser = getCurrentUser();
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findByRequestingUser(currentUser);
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<EquipmentReservationDTO> getReservationsByEventPublicId(UUID eventPublicId) {
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findByEvent_PublicId(eventPublicId);
        return reservations.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public String approveReservation(UUID reservationPublicId, String remarks) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not PENDING.";
        }

        Set<Role> currentUserRoles = currentUser.getRoles();
        if (!EQUIPMENT_APPROVER_ROLES.stream().anyMatch(currentUserRoles::contains)) {
            return "Error: You lack the required role to approve.";
        }

        // Specific check for EQUIPMENT_OWNER
        if (currentUserRoles.contains(Role.EQUIPMENT_OWNER)) {
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
                reservation,
                "received approval from " + currentUserRoles.iterator().next().name(),
                currentUser,
                "EQUIPMENT_RESERVATION_APPROVED");

        return "Equipment reservation approved by "
                + currentUserRoles.iterator().next().name()
                + ": "
                + currentUser.getFullName();
    }

    @Transactional
    public String rejectReservation(UUID reservationPublicId, String remarks) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));

        if (reservation.getStatus() != Status.PENDING) {
            return "Error: Reservation is not PENDING.";
        }

        Role currentUserRole = currentUser.getRoles().iterator().next();
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

        // Restore equipment quantity that was reserved during PENDING status
        Equipment equipment = reservation.getEquipment();
        Integer currentAvailable = equipment.getAvailableQuantity();
        Integer reservedQuantity = reservation.getQuantity();
        equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
        equipmentRepository.save(equipment);

        reservation.setStatus(Status.REJECTED);
        equipmentReservationRepository.save(reservation);

        notifyRequester(reservation, "rejected", currentUser, "EQUIPMENT_RESERVATION_REJECTED");

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
                approvals.stream()
                        .map(a -> a.getSignedBy().getRoles().iterator().next())
                        .collect(Collectors.toSet());

        boolean allRequiredApproved = approvingRoles.containsAll(REQUIRED_APPROVAL_ROLES);

        if (allRequiredApproved) {
            reservation.setStatus(Status.APPROVED);

            // Note: Equipment quantity was already decreased when reservation was created (PENDING)
            // No additional quantity adjustment needed during approval

            equipmentReservationRepository.save(reservation);
            notifyRequester(
                    reservation, "fully approved", null, "EQUIPMENT_RESERVATION_FULLY_APPROVED");
        }
    }

    @Transactional
    public String cancelReservation(UUID reservationPublicId) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));

        boolean isRequester =
                reservation.getRequestingUser().getPublicId().equals(currentUser.getPublicId());
        boolean isSuperAdmin = currentUser.getRoles().iterator().next() == Role.SUPER_ADMIN;

        if (!isRequester && !isSuperAdmin) {
            throw new SecurityException("You are not authorized to cancel this reservation.");
        }

        if (reservation.getStatus() == Status.CANCELED) {
            return "Warning: Reservation is already canceled.";
        }

        // Restore the inventory for both PENDING and APPROVED reservations
        // (since quantity was decreased when reservation was created)
        if (reservation.getStatus() == Status.APPROVED
                || reservation.getStatus() == Status.PENDING) {
            Equipment equipment = reservation.getEquipment();
            Integer currentAvailable = equipment.getAvailableQuantity();
            Integer reservedQuantity = reservation.getQuantity();

            equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
            equipmentRepository.save(equipment);
        }

        reservation.setStatus(Status.CANCELED);
        equipmentReservationRepository.save(reservation);
        notifyEquipmentOwnerOfCancellation(reservation, currentUser);
        notifyRequester(reservation, "canceled", currentUser, "EQUIPMENT_RESERVATION_CANCELED");

        return "Equipment reservation canceled successfully.";
    }

    @Transactional
    public void cancelReservationsForEvent(UUID eventPublicId, String reason) {
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findByEvent_PublicId(eventPublicId);
        for (EquipmentReservation reservation : reservations) {
            // If reservation was approved, restore the inventory
            if (reservation.getStatus() == Status.APPROVED) {
                Equipment equipment = reservation.getEquipment();
                Integer currentAvailable = equipment.getAvailableQuantity();
                Integer reservedQuantity = reservation.getQuantity();

                equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
                equipmentRepository.save(equipment);
            }

            reservation.setStatus(Status.CANCELED);
            equipmentReservationRepository.save(reservation);
        }
    }

    @Transactional
    public void deleteReservation(UUID reservationPublicId) {
        User currentUser = getCurrentUser();
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));

        boolean isRequester =
                reservation.getRequestingUser().getPublicId().equals(currentUser.getPublicId());
        boolean isSuperAdmin = currentUser.getRoles().iterator().next() == Role.SUPER_ADMIN;

        if (!isSuperAdmin
                && (!isRequester
                        || (reservation.getStatus() != Status.CANCELED
                                && reservation.getStatus() != Status.REJECTED))) {
            throw new SecurityException("Cannot delete this reservation.");
        }

        // If SUPER_ADMIN is deleting an approved reservation, restore inventory
        if (isSuperAdmin && reservation.getStatus() == Status.APPROVED) {
            Equipment equipment = reservation.getEquipment();
            Integer currentAvailable = equipment.getAvailableQuantity();
            Integer reservedQuantity = reservation.getQuantity();

            equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
            equipmentRepository.save(equipment);
        }

        equipmentReservationRepository.delete(reservation);
        logger.info("Deleted equipment reservation with public ID: {}", reservationPublicId);
    }

    @Transactional
    public void deleteReservationsByEventPublicId(UUID eventPublicId) {
        // Find all equipment reservations linked to this eventPublicId
        List<EquipmentReservation> reservations =
                equipmentReservationRepository.findByEvent_PublicId(eventPublicId);
        if (!reservations.isEmpty()) {
            // Restore inventory for any approved reservations before deleting
            for (EquipmentReservation reservation : reservations) {
                if (reservation.getStatus() == Status.APPROVED) {
                    Equipment equipment = reservation.getEquipment();
                    Integer currentAvailable = equipment.getAvailableQuantity();
                    Integer reservedQuantity = reservation.getQuantity();

                    equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
                    equipmentRepository.save(equipment);
                }
            }
            // If reservations are found, delete them
            equipmentReservationRepository.deleteAll(reservations);
            logger.info(
                    "Deleted {} equipment reservations for event public ID: {}",
                    reservations.size(),
                    eventPublicId);
        } else {
            logger.info(
                    "No equipment reservations found for event public ID: {} to delete.",
                    eventPublicId);
        }
    }

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

            notificationService.createNotification(
                    equipmentOwner,
                    message,
                    reservation.getEvent().getPublicId(),
                    reservation.getEvent().getPublicId(),
                    "EQUIPMENT_RESERVATION_REQUEST");

            System.out.println(
                    "DEBUG: Notify Equipment Owner: "
                            + equipmentOwner.getEmail()
                            + " - Message: "
                            + message);
        }
    }

    private void notifyRequester(
            EquipmentReservation reservation, String action, User actor, String notificationType) {
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

            notificationService.createNotification(
                    requester,
                    message,
                    reservation.getEvent().getPublicId(),
                    reservation.getPublicId(),
                    notificationType);

            System.out.println(
                    "DEBUG: Notify Requester: " + requester.getEmail() + " - Message: " + message);
        }
    }

    private void notifyEquipmentOwnerOfCancellation(
            EquipmentReservation reservation, User canceller) {
        User equipmentOwner = reservation.getEquipment().getEquipmentOwner();
        if (equipmentOwner != null
                && equipmentOwner.getEmail() != null
                && (canceller == null
                        || !equipmentOwner.getPublicId().equals(canceller.getPublicId()))) {
            String cancellerName = (canceller != null) ? canceller.getFullName() : "System";
            String message =
                    String.format(
                            "Equipment reservation for '%s' (Qty: %d, Event: %s) was canceled by"
                                    + " %s.",
                            reservation.getEquipment().getName(),
                            reservation.getQuantity(),
                            reservation.getEvent().getEventName(),
                            cancellerName);

            notificationService.createNotification(
                    equipmentOwner,
                    message,
                    reservation.getEvent().getPublicId(),
                    reservation.getPublicId(),
                    "EQUIPMENT_RESERVATION_CANCELED");

            System.out.println(
                    "DEBUG: Notify Owner of Cancellation: "
                            + equipmentOwner.getEmail()
                            + " - Message: "
                            + message);
        }
    }

    public EquipmentReservationDTO mapToDTO(EquipmentReservation reservation) {
        UserDTO requesterDto = mapUserToDTO(reservation.getRequestingUser());
        List<EquipmentApprovalDTO> approvalDTOs =
                reservation.getApprovals() != null
                        ? reservation.getApprovals().stream()
                                .map(this::mapApprovalToDTO)
                                .collect(Collectors.toList())
                        : List.of();

        EventDTO eventDto = null;
        if (reservation.getEvent() != null && eventMapper != null) {
            eventDto = eventMapper.toDto(reservation.getEvent());
        }
        DepartmentDTO departmentDto = null;
        if (reservation.getDepartment() != null && departmentMapper != null) {
            departmentDto = departmentMapper.toDto(reservation.getDepartment());
        }
        EquipmentDTO equipmentDto = null;
        if (reservation.getEquipment() != null && equipmentMapper != null) {
            equipmentDto = equipmentMapper.toDto(reservation.getEquipment());
        }

        return new EquipmentReservationDTO(
                reservation.getPublicId(),
                eventDto,
                requesterDto,
                departmentDto,
                equipmentDto,
                reservation.getQuantity(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus().name(),
                approvalDTOs,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }

    private EquipmentApprovalDTO mapApprovalToDTO(EquipmentApproval approval) {
        User signedByEntity = approval.getSignedBy();
        UserDTO signedByUserDto = mapUserToDTO(signedByEntity);

        String userRole = null;
        if (signedByEntity != null && signedByEntity.getRoles() != null) {
            userRole = signedByEntity.getRoles().iterator().next().name();
        }

        return new EquipmentApprovalDTO(
                approval.getPublicId(),
                approval.getEquipmentReservation().getPublicId(),
                signedByUserDto,
                Set.of(userRole),
                approval.getRemarks(),
                approval.getStatus().name(),
                approval.getDateSigned());
    }

    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
            try {
                profileImageUrl =
                        fileStorageService.getFileUrl(user.getProfileImagePath(), usersBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for user "
                                + user.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        Department userDepartmentEntity = user.getDepartment();
        DepartmentDTO departmentDto = null;
        if (userDepartmentEntity != null && departmentMapper != null) {
            departmentDto = departmentMapper.toDto(userDepartmentEntity);
        }

        return new UserDTO(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles() != null
                        ? Set.of(user.getRoles().iterator().next().name())
                        : new HashSet<>(),
                departmentDto,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // --- Getters for specific lists ---
    public List<EquipmentApprovalDTO> getAllApprovalsForReservation(UUID reservationPublicId) {
        EquipmentReservation reservation =
                equipmentReservationRepository
                        .findByPublicId(reservationPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment Reservation not found with Public ID: "
                                                        + reservationPublicId));
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

    @Transactional
    public Map<UUID, String> bulkApproveReservations(
            List<UUID> reservationPublicIds, String remarks) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID reservationId : reservationPublicIds) {
            try {
                String result = approveReservation(reservationId, remarks);
                results.put(reservationId, result);
            } catch (Exception e) {
                results.put(reservationId, "Error: " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public Map<UUID, String> bulkRejectReservations(
            List<UUID> reservationPublicIds, String remarks) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID reservationId : reservationPublicIds) {
            try {
                String result = rejectReservation(reservationId, remarks);
                results.put(reservationId, result);
            } catch (Exception e) {
                results.put(reservationId, "Error: " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public Map<UUID, String> bulkCancelReservations(List<UUID> reservationPublicIds) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID reservationId : reservationPublicIds) {
            try {
                String result = cancelReservation(reservationId);
                results.put(reservationId, result);
            } catch (Exception e) {
                results.put(reservationId, "Error: " + e.getMessage());
            }
        }
        return results;
    }

    @Deprecated
    @Transactional
    public void returnEquipmentForCompletedEvent(UUID eventPublicId) {
        logger.info("Returning equipment for completed event: {}", eventPublicId);

        Event event =
                eventRepository
                        .findByPublicId(eventPublicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: "
                                                        + eventPublicId));

        // Check if event has actually ended, regardless of status
        Instant now = Instant.now();
        if (event.getEndTime().isAfter(now)) {
            logger.warn(
                    "Event {} has not ended yet (End time: {}), skipping equipment return. Current"
                            + " time: {}",
                    eventPublicId,
                    event.getEndTime(),
                    now);
            return;
        }

        // Find all active reservations (APPROVED or ONGOING) for this event
        List<EquipmentReservation> activeReservations =
                equipmentReservationRepository.findByEvent_PublicId(eventPublicId).stream()
                        .filter(
                                reservation ->
                                        reservation.getStatus() == Status.APPROVED
                                                || reservation.getStatus() == Status.ONGOING)
                        .collect(Collectors.toList());

        if (activeReservations.isEmpty()) {
            logger.info("No active equipment reservations found for event: {}", eventPublicId);
            return;
        }

        int returnedItems = 0;
        for (EquipmentReservation reservation : activeReservations) {
            Equipment equipment = reservation.getEquipment();
            Integer reservedQuantity = reservation.getQuantity();
            Integer currentAvailable = equipment.getAvailableQuantity();

            // Restore the reserved quantity back to available inventory
            equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
            equipmentRepository.save(equipment);

            // Update reservation status to COMPLETED to avoid double restoration
            reservation.setStatus(Status.COMPLETED);
            equipmentReservationRepository.save(reservation);

            logger.info(
                    "Returned {} units of equipment '{}' (ID: {}) from event '{}' (Event end time:"
                            + " {})",
                    reservedQuantity,
                    equipment.getName(),
                    equipment.getPublicId(),
                    event.getEventName(),
                    event.getEndTime());

            returnedItems++;
        }

        logger.info(
                "Successfully returned {} equipment items for completed event: {}",
                returnedItems,
                eventPublicId);

        // Optional: Notify the event organizer about equipment return
        if (event.getOrganizer() != null) {
            notificationService.createNotification(
                    event.getOrganizer(),
                    "Equipment for your completed event '"
                            + event.getEventName()
                            + "' has been automatically returned to inventory ("
                            + returnedItems
                            + " items).",
                    event.getPublicId(),
                    event.getPublicId(),
                    "EQUIPMENT_RETURNED");
        }
    }

    @Transactional
    public void restoreEquipmentForExpiredReservations(Instant currentTime) {
        logger.info(
                "Checking for equipment reservations to restore based on end time: {}",
                currentTime);

        // Find all active reservations (APPROVED or ONGOING) that have ended using database query
        List<EquipmentReservation> expiredReservations =
                equipmentReservationRepository.findExpiredActiveReservations(currentTime);

        if (expiredReservations.isEmpty()) {
            logger.info("No expired equipment reservations found to restore.");
            return;
        }

        int restoredItems = 0;
        for (EquipmentReservation reservation : expiredReservations) {
            try {
                Equipment equipment = reservation.getEquipment();
                Integer reservedQuantity = reservation.getQuantity();
                Integer currentAvailable = equipment.getAvailableQuantity();

                // Restore the reserved quantity back to available inventory
                equipment.setAvailableQuantity(currentAvailable + reservedQuantity);
                equipmentRepository.save(equipment);

                // Update reservation status to COMPLETED to avoid double restoration
                reservation.setStatus(Status.CANCELED);
                equipmentReservationRepository.save(reservation);

                logger.info(
                        "Restored {} units of equipment '{}' (ID: {}) from expired reservation (End"
                                + " time: {})",
                        reservedQuantity,
                        equipment.getName(),
                        equipment.getPublicId(),
                        reservation.getEndTime());

                restoredItems++;

                // Notify the requesting user about automatic equipment return
                if (reservation.getRequestingUser() != null) {
                    notificationService.createNotification(
                            reservation.getRequestingUser(),
                            "Equipment '"
                                    + equipment.getName()
                                    + "' ("
                                    + reservedQuantity
                                    + " units) has been automatically "
                                    + "returned to inventory as your reservation period has ended.",
                            reservation.getPublicId(),
                            reservation.getEvent().getPublicId(),
                            "EQUIPMENT_AUTO_RETURNED");
                }

            } catch (Exception e) {
                logger.error(
                        "Error restoring equipment for reservation {}: {}",
                        reservation.getPublicId(),
                        e.getMessage(),
                        e);
            }
        }

        logger.info(
                "Successfully restored {} equipment items from {} expired reservations.",
                restoredItems,
                expiredReservations.size());
    }
}
