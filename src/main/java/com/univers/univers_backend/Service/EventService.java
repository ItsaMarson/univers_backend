/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Entity.*;
import com.univers.univers_backend.Enum.ErrorMessage;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.DepartmentMapper;
import com.univers.univers_backend.Mapper.EventMapper;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Mapper.VenueMapper;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final DepartmentService departmentService;
    private final EventApprovalRepository eventApprovalRepository;
    private final EquipmentReservationService equipmentReservationService;

    // Mappers
    private final EventMapper eventMapper;

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Value("${minio.bucket.approved-letters}")
    private String lettersBucketName;

    @Value("${minio.bucket.events}")
    private String eventsBucketName;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            VenueRepository venueRepository,
            FileStorageService fileStorageService,
            DepartmentService departmentService,
            NotificationService notificationService,
            EventApprovalRepository eventApprovalRepository,
            EquipmentReservationService equipmentReservationService,
            @Lazy EventMapper eventMapper,
            @Lazy UserMapper userMapper,
            @Lazy VenueMapper venueMapper,
            @Lazy DepartmentMapper departmentMapper) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.fileStorageService = fileStorageService;
        this.departmentService = departmentService;
        this.notificationService = notificationService;
        this.eventApprovalRepository = eventApprovalRepository;
        this.equipmentReservationService = equipmentReservationService;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public EventDTO createEvent(
            CreateEventRequestDTO requestDTO,
            MultipartFile approvedLetterFile,
            MultipartFile eventImageFile) {

        User organizer = getCurrentUser();

        Venue venue =
                venueRepository
                        .findByPublicId(requestDTO.venuePublicId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid venue Public ID: "
                                                        + requestDTO.venuePublicId()));

        Department eventDepartment = null;
        if (requestDTO.departmentPublicId() != null) {
            eventDepartment =
                    departmentService.getDepartmentByPublicId(requestDTO.departmentPublicId());
            if (eventDepartment == null) {
                throw new IllegalArgumentException(
                        "Invalid department Public ID: " + requestDTO.departmentPublicId());
            }
        } else {
            logger.warn(
                    "No specific department Public ID provided for event, attempting to use"
                            + " organizer's department.");
            eventDepartment = organizer.getDepartment();
        }

        List<Event> conflictingEvents =
                eventRepository.findConflictingEvents(
                        venue.getId(), requestDTO.startTime(), requestDTO.endTime());
        if (!conflictingEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "There is a scheduling conflict with another event at this venue and time.");
        }

        Event event = new Event();
        event.setEventName(requestDTO.eventName());
        event.setEventType(requestDTO.eventType());
        event.setStartTime(requestDTO.startTime());
        event.setEndTime(requestDTO.endTime());
        event.setDepartment(eventDepartment);
        event.setOrganizer(organizer);
        event.setEventVenue(venue);
        event.setStatus(Status.PENDING);

        if (approvedLetterFile != null && !approvedLetterFile.isEmpty()) {
            String letterObjectName =
                    fileStorageService.uploadFile(
                            approvedLetterFile, lettersBucketName, "approved-letters/");
            event.setApprovedLetterPath(letterObjectName);
        }
        if (eventImageFile != null && !eventImageFile.isEmpty()) {
            String imageObjectName =
                    fileStorageService.uploadFile(
                            eventImageFile, eventsBucketName, "event-images/");
            event.setImagePath(imageObjectName);
        }

        Event savedEvent = eventRepository.save(event);

        Set<User> processedApprovers = new HashSet<>();

        // 1. Venue Owner
        Venue eventVenue = savedEvent.getEventVenue();
        if (eventVenue != null && eventVenue.getVenueOwner() != null) {
            User venueOwner = eventVenue.getVenueOwner();
            if (processedApprovers.add(venueOwner)) {
                EventApproval venueApproval = new EventApproval();
                venueApproval.setEvent(savedEvent);
                venueApproval.setSignedBy(venueOwner);
                venueApproval.setStatus(Status.PENDING);
                eventApprovalRepository.save(venueApproval);
            }
            if (!venueOwner.getPublicId().equals(organizer.getPublicId())) {
                notificationService.createNotification(
                        venueOwner,
                        "A new event '"
                                + savedEvent.getEventName()
                                + "' has been proposed for your venue '"
                                + eventVenue.getName()
                                + "' and requires your approval.",
                        savedEvent.getPublicId(),
                        savedEvent.getPublicId(),
                        "VENUE_RESERVATION_APPROVAL");
            }
        }

        // 2. Department Head
        Department eventDept = savedEvent.getDepartment();
        if (eventDept != null && eventDept.getDeptHead() != null) {
            User deptHead = eventDept.getDeptHead();
            if (processedApprovers.add(deptHead)) {
                EventApproval deptApproval = new EventApproval();
                deptApproval.setEvent(savedEvent);
                deptApproval.setSignedBy(deptHead);
                deptApproval.setStatus(Status.PENDING);
                eventApprovalRepository.save(deptApproval);

                if (!deptHead.getPublicId().equals(organizer.getPublicId())) {
                    notificationService.createNotification(
                            deptHead,
                            "A new event '"
                                    + savedEvent.getEventName()
                                    + "' has been proposed under your department '"
                                    + eventDept.getName()
                                    + "' and requires your approval.",
                            savedEvent.getPublicId(),
                            savedEvent.getPublicId(),
                            "DEPARTMENT_EVENT_APPROVAL");
                }
            }
        }

        // 3. Event Approvers (Role-based)
        List<User> eventApprovers = userRepository.findAllByRolesContains(Role.ADMIN);
        for (User approver : eventApprovers) {
            if (processedApprovers.add(approver)) {
                EventApproval roleApproval = new EventApproval();
                roleApproval.setEvent(savedEvent);
                roleApproval.setSignedBy(approver);
                roleApproval.setStatus(Status.PENDING);
                eventApprovalRepository.save(roleApproval);

                if (!approver.getPublicId().equals(organizer.getPublicId())) {
                    notificationService.createNotification(
                            approver,
                            "A new event '"
                                    + savedEvent.getEventName()
                                    + "' requires your approval as a designated Event Approver.",
                            savedEvent.getPublicId(),
                            savedEvent.getPublicId(),
                            "EVENT_APPROVAL_REQUEST");
                }
            }
        }

        return eventMapper.toDto(savedEvent);
    }

    public List<EventDTO> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public EventDTO getEventByPublicId(UUID publicId) {
        Event event =
                eventRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: " + publicId));
        return eventMapper.toDto(event);
    }

    public List<EventDTO> getApprovedEventsByVenue(UUID venuePublicId) {
        List<Event> events =
                eventRepository.findByStatusAndEventVenue_PublicId(Status.APPROVED, venuePublicId);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public List<EventDTO> getOngoingAndApprovedEventsByVenue(UUID venuePublicId) {
        List<Status> statuses = List.of(Status.APPROVED, Status.ONGOING);
        List<Event> events =
                eventRepository.findByStatusInAndEventVenue_PublicId(statuses, venuePublicId);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EventDTO updateEvent(
            UUID publicId,
            UpdateEventRequestDTO requestDTO,
            MultipartFile approvedLetterFile,
            MultipartFile eventImageFile) {

        Event event =
                eventRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: " + publicId));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRoles().contains(Role.SUPER_ADMIN);
        boolean isOrganizer = event.getOrganizer().getPublicId().equals(currentUser.getPublicId());
        if (!isAdmin && !isOrganizer) {
            throw new SecurityException("User not authorized to update this event.");
        }

        if (!isAdmin && event.getStatus() != Status.PENDING) {
            throw new IllegalStateException(
                    "Event cannot be updated because it is not in PENDING status. Current status: "
                            + event.getStatus());
        }

        if (requestDTO.eventName() != null && !requestDTO.eventName().isBlank()) {
            event.setEventName(requestDTO.eventName());
        }
        if (requestDTO.eventType() != null && !requestDTO.eventType().isBlank()) {
            event.setEventType(requestDTO.eventType());
        }
        if (requestDTO.startTime() != null) {
            event.setStartTime(requestDTO.startTime());
        }
        if (requestDTO.endTime() != null) {
            event.setEndTime(requestDTO.endTime());
        }


        if (requestDTO.organizerPublicId() != null
                && !event.getOrganizer().getPublicId().equals(requestDTO.organizerPublicId())) {
            User newOrganizer =
                    userRepository
                            .findByPublicId(requestDTO.organizerPublicId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "New organizer not found with Public ID: "
                                                            + requestDTO.organizerPublicId()));
            event.setOrganizer(newOrganizer);
        }
        if (requestDTO.venuePublicId() != null
                && (event.getEventVenue() == null
                        || !event.getEventVenue()
                                .getPublicId()
                                .equals(requestDTO.venuePublicId()))) {
            Venue newVenue =
                    venueRepository
                            .findByPublicId(requestDTO.venuePublicId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Venue not found with Public ID: "
                                                            + requestDTO.venuePublicId()));
            event.setEventVenue(newVenue);
        }
        if (requestDTO.departmentPublicId() != null) {
            if (event.getDepartment() == null
                    || !requestDTO
                            .departmentPublicId()
                            .equals(event.getDepartment().getPublicId())) {
                Department newDepartment =
                        departmentService.getDepartmentByPublicId(requestDTO.departmentPublicId());
                if (newDepartment == null) {
                    throw new IllegalArgumentException(
                            "Department not found with Public ID: "
                                    + requestDTO.departmentPublicId());
                }
                event.setDepartment(newDepartment);
            }
        }

        if (approvedLetterFile != null && !approvedLetterFile.isEmpty()) {
            deleteFileSafely(
                    event.getApprovedLetterPath(),
                    lettersBucketName,
                    publicId,
                    "old approved letter");
            String letterObjectName =
                    fileStorageService.uploadFile(
                            approvedLetterFile, lettersBucketName, "approved-letters/");
            event.setApprovedLetterPath(letterObjectName);
        }
        if (eventImageFile != null && !eventImageFile.isEmpty()) {
            deleteFileSafely(event.getImagePath(), eventsBucketName, publicId, "old image");
            String imageObjectName =
                    fileStorageService.uploadFile(
                            eventImageFile, eventsBucketName, "event-images/");
            event.setImagePath(imageObjectName);
        }

        if (requestDTO.status() != null) {
            event.setStatus(requestDTO.status());
        }

        Event updatedDbEvent = eventRepository.save(event);
        return eventMapper.toDto(updatedDbEvent);
    }

    @Transactional
    public void deleteEvent(UUID publicId) {
        User currentUser = getCurrentUser();
        Event event =
                eventRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: " + publicId));

        if (currentUser.getRoles().contains(Role.SUPER_ADMIN)) {
            logger.info("SUPER_ADMIN deleting event {}. Performing thorough deletion.", publicId);

            List<EventApproval> approvals = eventApprovalRepository.findAllByEvent(event);
            if (!approvals.isEmpty()) {
                eventApprovalRepository.deleteAll(approvals);
                logger.info(
                        "Deleted {} approval records for event {}.", approvals.size(), publicId);
            }

            deleteEventFiles(event);
            eventRepository.delete(event);
            logger.info("Event {} deleted successfully by SUPER_ADMIN.", publicId);

        } else {
            boolean isOrganizer =
                    event.getOrganizer().getPublicId().equals(currentUser.getPublicId());
            boolean isPending = event.getStatus() == Status.PENDING;
            boolean hasNoApprovals = eventApprovalRepository.findAllByEvent(event).isEmpty();

            if (isOrganizer && isPending && hasNoApprovals) {
                logger.info(
                        "Organizer {} deleting PENDING event {} with no approvals.",
                        currentUser.getPublicId(),
                        publicId);
                deleteEventFiles(event);
                eventRepository.delete(event);
                logger.info(
                        "Event {} deleted successfully by organizer {}.",
                        publicId,
                        currentUser.getPublicId());
            } else {
                String reason = "User not authorized to delete this event.";
                if (!isOrganizer) reason = "User is not the organizer.";
                else if (!isPending) reason = "Event is not in PENDING state.";
                else if (!hasNoApprovals) reason = "Event has existing approvals.";
                logger.warn("Failed attempt to delete event {}: {}", publicId, reason);
                throw new SecurityException(reason);
            }
        }
    }

    // Helper method to delete event files
    private void deleteEventFiles(Event event) {
        deleteFileSafely(
                event.getApprovedLetterPath(),
                lettersBucketName,
                event.getPublicId(),
                "approved letter");
        deleteFileSafely(
                event.getImagePath(), eventsBucketName, event.getPublicId(), "event image");
    }

    // Helper method to safely delete a single file
    private void deleteFileSafely(
            String filePath, String bucketName, UUID eventPublicId, String fileType) {
        if (filePath != null && !filePath.isBlank()) {
            try {
                fileStorageService.deleteFile(filePath, bucketName);
                logger.info(
                        "Successfully deleted {} for event {}: {}",
                        fileType,
                        eventPublicId,
                        filePath);
            } catch (Exception e) {
                logger.error(
                        "Error deleting {} for event {} (Path: {}): {}",
                        fileType,
                        eventPublicId,
                        filePath,
                        e.getMessage());
            }
        }
    }

    @Transactional
    public String cancelEvent(UUID publicId, String cancellationReason) {
        User currentUser = getCurrentUser();
        Event event =
                eventRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with public ID: " + publicId));

        boolean isAdmin = currentUser.getRoles().contains(Role.SUPER_ADMIN);
        boolean isOrganizer = event.getOrganizer().getPublicId().equals(currentUser.getPublicId());

        if (!isAdmin && !isOrganizer) {
            throw new SecurityException("User not authorized to cancel this event.");
        }

        if (event.getStatus() == Status.CANCELED) return "Event is already canceled.";
        if (event.getStatus() == Status.COMPLETED || event.getStatus() == Status.ONGOING) {
            throw new IllegalStateException(
                    "Cannot cancel an event that is ongoing or already completed.");
        }

        event.setStatus(Status.CANCELED);
        Event canceledEvent = eventRepository.save(event);

        String reasonOrDefault =
                cancellationReason != null ? cancellationReason : "Event canceled by user.";

        try {
            equipmentReservationService.cancelReservationsForEvent(
                    canceledEvent.getPublicId(), reasonOrDefault);
            logger.info(
                    "Initiated cancellation for equipment reservations associated with event {}.",
                    canceledEvent.getPublicId());
        } catch (Exception e) {
            logger.error(
                    "Error during cancellation of equipment reservations for event {}: {}",
                    canceledEvent.getPublicId(),
                    e.getMessage(),
                    e);
            throw new RuntimeException(
                    "Error during cancellation of equipment reservations for event "
                            + canceledEvent.getPublicId()
                            + ": "
                            + e.getMessage(),
                    e);
        }

        notificationService.createNotification(
                event.getOrganizer(),
                "Your event '"
                        + event.getEventName()
                        + "' has been canceled. Reason: "
                        + reasonOrDefault,
                event.getPublicId(),
                event.getPublicId(),
                "EVENT_CANCELED");

        // Notify venue owner if applicable
        Venue venue = canceledEvent.getEventVenue();
        if (venue != null && venue.getVenueOwner() != null) {
            User venueOwner = venue.getVenueOwner();
            notificationService.createNotification(
                    venueOwner,
                    "The event '"
                            + canceledEvent.getEventName()
                            + "' scheduled at your venue '"
                            + venue.getName()
                            + "' has been canceled. Reason: "
                            + reasonOrDefault,
                    canceledEvent.getPublicId(),
                    null, // No specific reservation ID anymore
                    "EVENT_VENUE_CANCELLATION_INFO");
        }

        return "Event canceled successfully.";
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

    public EventDTO mapToDTO(Event event) {
        if (event == null) {
            return null;
        }
        return this.eventMapper.toDto(event);
    }

    @Transactional(readOnly = true)
    public List<EventDTO> searchEvents(
            String scope,
            String statusString,
            String sortBy,
            String startDateStr,
            String endDateStr) {
        User currentUser = getCurrentUser();
        Set<Role> userRole = currentUser.getRoles();

        // 1. Parse Status Filter
        Status statusFilter = null;
        if (statusString != null && !statusString.equalsIgnoreCase("ALL")) {
            try {
                statusFilter = Status.valueOf(statusString.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn(
                        "Invalid status value provided: {}, ignoring status filter.", statusString);
            }
        }

        // 2. Parse Sorting
        Sort sort;
        if ("recency".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("date".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "startTime");
        } else {
            // Default sort, e.g., by start time ascending
            sort = Sort.by(Sort.Direction.ASC, "startTime");
        }

        // 3. Parse Date Range Filter (now applies to event startTime/endTime)
        Instant queryStartDate = null;
        Instant queryEndDate = null;

        try {
            if (startDateStr != null && !startDateStr.isEmpty()) {
                queryStartDate = Instant.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                queryEndDate = Instant.parse(endDateStr);
            }
        } catch (DateTimeParseException e) {
            logger.error(
                    "Invalid date format provided for searchEvents: {} or {}",
                    startDateStr,
                    endDateStr,
                    e);
            throw new IllegalArgumentException(
                    "Invalid date format. Please use ISO 8601 format (e.g., YYYY-MM-DDTHH:mm:ssZ).",
                    e);
        }

        // Create final variables for use in lambdas
        final Status finalStatusFilter = statusFilter;
        final Instant finalQueryStartDate = queryStartDate;
        final Instant finalQueryEndDate = queryEndDate;

        // 4. Build Specification
        Specification<Event> spec = Specification.where(null); // Start with a neutral specification

        // Apply scope-based filtering
        switch (scope.toLowerCase()) {
            case "mine":
                spec = spec.and((root, query, cb) -> cb.equal(root.get("organizer"), currentUser));
                break;

            case "related":
                // Check if user is a designated approver
                if (userRole.contains(Role.ADMIN)
                        || userRole.contains(Role.VP_ADMIN)
                        || userRole.contains(Role.DEPT_HEAD)
                        || userRole.contains(Role.VENUE_OWNER)
                        || userRole.contains(Role.EQUIPMENT_OWNER)
                        || userRole.contains(Role.VPAA)) {
                    // Get all events where this user is a designated approver
                    spec =
                            spec.and(
                                    (root, query, cb) -> {
                                        // Join with EventApproval to find events where this user is
                                        // an approver
                                        Subquery<Long> subquery = query.subquery(Long.class);
                                        Root<EventApproval> approvalRoot =
                                                subquery.from(EventApproval.class);

                                        return cb.exists(
                                                subquery.select(cb.literal(1L))
                                                        .where(
                                                                cb.and(
                                                                        cb.equal(
                                                                                approvalRoot.get(
                                                                                        "event"),
                                                                                root),
                                                                        cb.equal(
                                                                                approvalRoot.get(
                                                                                        "signedBy"),
                                                                                currentUser)
                                                                        // cb.equal(approvalRoot.get("status"), Status.PENDING)
                                                                        )));
                                    });
                } else {
                    logger.info(
                            "User role {} cannot query for scope 'related'. Returning empty list.",
                            userRole);
                    return List.of();
                }
                break;

            case "all":
                List<Status> allowedStatuses =
                        List.of(Status.APPROVED, Status.ONGOING, Status.PENDING);
                spec = spec.and((root, query, cb) -> root.get("status").in(allowedStatuses));
                break;

            case "approved":
            default:
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), Status.APPROVED));
                break;
        }

        // Apply optional status filter (if scope didn't already enforce a status like 'approved')
        if (finalStatusFilter != null) {
            if (scope.equalsIgnoreCase("approved") && finalStatusFilter != Status.APPROVED) {
                logger.warn(
                        "Status filter {} ignored for 'approved' scope (shows only APPROVED).",
                        statusString);
            } else if (scope.equalsIgnoreCase("all")) {
                Set<Status> allowedInAll = Set.of(Status.APPROVED, Status.ONGOING, Status.PENDING);
                if (allowedInAll.contains(finalStatusFilter)) {
                    spec =
                            spec.and(
                                    (root, query, cb) ->
                                            cb.equal(root.get("status"), finalStatusFilter));
                } else {
                    logger.warn(
                            "Status filter {} ignored for 'all' scope (allowed: APPROVED, ONGOING,"
                                    + " PENDING).",
                            statusString);
                }
            } else {
                spec =
                        spec.and(
                                (root, query, cb) ->
                                        cb.equal(root.get("status"), finalStatusFilter));
            }
        }

        // Apply optional date range filter to event's startTime and endTime
        if (finalQueryStartDate != null && finalQueryEndDate != null) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.and(
                                            cb.lessThanOrEqualTo(
                                                    root.get("startTime"),
                                                    finalQueryEndDate), // Event starts before or at
                                            cb.greaterThanOrEqualTo(
                                                    root.get("endTime"),
                                                    finalQueryStartDate) // Event ends after or at
                                            ));
        } else if (finalQueryStartDate != null) {
            // If only startDate is provided, find events that end on or after startDate
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.greaterThanOrEqualTo(
                                            root.get("endTime"), finalQueryStartDate));
        } else if (finalQueryEndDate != null) {
            // If only endDate is provided, find events that start on or before endDate
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.lessThanOrEqualTo(root.get("startTime"), finalQueryEndDate));
        }

        // 5. Execute Query
        List<Event> events = eventRepository.findAll(spec, sort);

        // 6. Map to DTOs
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventDTO> getTimelineEventsByDateRange(String startDateStr, String endDateStr) {
        Instant startDate = null;
        Instant endDate = null;

        try {
            if (startDateStr != null && !startDateStr.isEmpty()) {
                startDate = Instant.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                endDate = Instant.parse(endDateStr);
            }
        } catch (DateTimeParseException e) {
            logger.error(
                    "Invalid date format provided for timeline: {} or {}",
                    startDateStr,
                    endDateStr,
                    e);
            throw new IllegalArgumentException(
                    "Invalid date format. Please use ISO 8601 format (e.g., YYYY-MM-DDTHH:mm:ssZ).",
                    e);
        }

        // Define statuses to include (exclude PENDING, CANCELED, REJECTED)
        List<Status> includedStatuses = List.of(Status.APPROVED, Status.COMPLETED, Status.ONGOING);

        // Create final variables for use in lambdas
        final Instant finalStartDate = startDate;
        final Instant finalEndDate = endDate;

        Specification<Event> spec = Specification.where(null);

        // Filter by statuses
        spec = spec.and((root, query, cb) -> root.get("status").in(includedStatuses));

        // Filter by date range (event overlaps with the given range)
        if (finalStartDate != null && finalEndDate != null) {
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.and(
                                            cb.lessThanOrEqualTo(
                                                    root.get("startTime"),
                                                    finalEndDate), // Event starts before or at
                                            cb.greaterThanOrEqualTo(
                                                    root.get("endTime"),
                                                    finalStartDate) // Event ends after or at query
                                            ));
        } else if (finalStartDate != null) {
            // If only startDate is provided, find events that end on or after startDate
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.greaterThanOrEqualTo(root.get("endTime"), finalStartDate));
        } else if (finalEndDate != null) {
            // If only endDate is provided, find events that start on or before endDate
            spec =
                    spec.and(
                            (root, query, cb) ->
                                    cb.lessThanOrEqualTo(root.get("startTime"), finalEndDate));
        }
        // If neither startDate nor endDate is provided, it fetches all events with the
        // includedStatuses.

        // Sort by start time
        Sort sort = Sort.by(Sort.Direction.ASC, "startTime");

        List<Event> events = eventRepository.findAll(spec, sort);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public List<EventPersonnelDTO> addPersonnel(UUID eventPublicId, EventPersonnelDTO requestDTO){
        User currentUser = getCurrentUser();

        Event event = eventRepository.findByPublicId(eventPublicId).orElseThrow(()-> new NoSuchElementException("Event not found with UUID" + eventPublicId));

        if(!(currentUser.getRoles().contains(Role.EQUIPMENT_OWNER) || currentUser.getRoles().contains(Role.SUPER_ADMIN))){
            throw new AccessDeniedException("User not authorized to add personnel");
        }
        if(requestDTO == null){
            throw new IllegalArgumentException("Request body cannot be null");
        }

        EventPersonnel newPersonnel = new EventPersonnel();
        newPersonnel.setName(requestDTO.name());
        newPersonnel.setPhoneNumber(requestDTO.phoneNumber());
        newPersonnel.setStatus(requestDTO.status() != null ? requestDTO.status() : Status.AVAILABLE);

        if(event.getAssignedPersonnel() == null){
            event.setAssignedPersonnel(new ArrayList<>());
        }
        event.getAssignedPersonnel().add(newPersonnel);
        Event updatedEvent = eventRepository.save(event);

        return updatedEvent.getAssignedPersonnel().stream()
                .map(eventMapper::toPersonnelDto)
                .collect(Collectors.toList());

    }
    public void deletePersonnel(UUID eventPublicId, UUID personnelPublicId){
        User currentUser = getCurrentUser();

        Event event = eventRepository.findByPublicId(eventPublicId).orElseThrow(()-> new NoSuchElementException("Event not found with UUID" + eventPublicId));

        if(!(currentUser.getRoles().contains(Role.EQUIPMENT_OWNER) || currentUser.getRoles().contains(Role.SUPER_ADMIN))){
            throw new SecurityException("User not authorized to add personnel");
        }
        List<EventPersonnel> assignedPersonnel = event.getAssignedPersonnel();
        if (assignedPersonnel == null || assignedPersonnel.isEmpty()) {
            throw new NoSuchElementException("Personnel not found with UUID " + personnelPublicId);
        }

        Iterator<EventPersonnel> iterator = assignedPersonnel.iterator();
        boolean foundAndRemoved = false;

        while (iterator.hasNext()){
            EventPersonnel personnel = iterator.next();
            if(personnel.getPublicId().equals(personnelPublicId)){
                iterator.remove();
                foundAndRemoved = true;
                break;
            }
        }
        if(!foundAndRemoved){
            throw new NoSuchElementException("Personnel not found with UUID" + personnelPublicId);
        }
        eventRepository.save(event);
    }
}
