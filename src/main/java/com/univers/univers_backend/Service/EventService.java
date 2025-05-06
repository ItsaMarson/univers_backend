/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueReservationDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final VenueReservationService venueReservationService;
    private final DepartmentService departmentService;
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    @Value("${minio.bucket.letters}")
    private String lettersBucketName;

    @Value("${minio.bucket.events}")
    private String eventsBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            VenueRepository venueRepository,
            FileStorageService fileStorageService,
            VenueReservationService venueReservationService,
            DepartmentService departmentService,
            NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.fileStorageService = fileStorageService;
        this.venueReservationService = venueReservationService;
        this.departmentService = departmentService;
        this.notificationService = notificationService;
    }

    @Transactional
    public EventDTO createEvent(
            EventDTO eventDTO, MultipartFile approvedLetterFile, MultipartFile eventImageFile) {

        Long organizerId = eventDTO.organizer().id();
        User organizer =
                userRepository
                        .findById(organizerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Organizer not found with ID: " + organizerId));

        Venue venue =
                venueRepository
                        .findById(eventDTO.eventVenueId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid venue ID: " + eventDTO.eventVenueId()));

        Long eventDepartmentId = eventDTO.departmentId();
        Department eventDepartment = null;
        if (eventDepartmentId != null) {
            eventDepartment = departmentService.getDepartmentById(eventDepartmentId);
            if (eventDepartment == null) {
                throw new IllegalArgumentException("Invalid department ID: " + eventDepartmentId);
            }
        } else {
            // Fallback to organizer's department if no specific departmentId is provided for the
            // event
            // Or throw an error if departmentId is strictly required for an event
            // For now, let's assume it's optional and can fallback or be null
            // If it's mandatory, uncomment the error or ensure eventDTO.departmentId() is always
            // present
            // throw new IllegalArgumentException("Department ID is required for the event.");
            System.err.println(
                    "Warning: No specific departmentId provided for event, attempting to use"
                            + " organizer's department for reservation.");
        }

        List<Event> conflictingEvents =
                eventRepository.findConflictingEvents(
                        venue.getId(), eventDTO.startTime(), eventDTO.endTime());
        if (!conflictingEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "There is a scheduling conflict with another event at this venue and time.");
        }
        try {
            Long departmentIdForTempCheck =
                    eventDepartmentId != null
                            ? eventDepartmentId
                            : (organizer.getDepartment() != null
                                    ? organizer.getDepartment().getId()
                                    : null);

            VenueReservationDTO tempReservationCheckDto =
                    new VenueReservationDTO(
                            null, // id
                            null, // eventId (not created yet)
                            null, // eventName
                            null, // requestingUser
                            departmentIdForTempCheck,
                            null, // departmentName
                            venue.getId(), // venueId
                            null, // venueName
                            eventDTO.startTime(), // startTime
                            eventDTO.endTime(), // endTime
                            null, // status
                            null, // approvals
                            null, // createdAt
                            null // updatedAt
                            );
            // Call a hypothetical conflict check method (or adapt createVenueReservation to allow
            // checks)
            // This part might require adjustment in VenueReservationService or its repository
            // For now, we rely on the check within the actual createVenueReservation call later.
            // If VenueReservationService.createVenueReservation throws due to conflict, the
            // @Transactional will rollback the event.

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Venue conflict detected: " + e.getMessage());
        }

        Event event = new Event();
        event.setEventName(eventDTO.eventName());
        event.setEventType(eventDTO.eventType());
        event.setStartTime(eventDTO.startTime());
        event.setEndTime(eventDTO.endTime());
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

        try {
            // Use the event's specified departmentId for the reservation if available,
            // otherwise fallback to the organizer's department.
            Long departmentIdForReservation = eventDepartmentId;
            String departmentNameForReservation = null;

            if (departmentIdForReservation != null && eventDepartment != null) {
                departmentNameForReservation = eventDepartment.getName();
            } else {
                // Fallback to organizer's department if event specific one is not set
                Department organizerDept = organizer.getDepartment();
                if (organizerDept != null) {
                    departmentIdForReservation = organizerDept.getId();
                    departmentNameForReservation = organizerDept.getName();
                }
            }

            if (departmentIdForReservation == null) {
                System.err.println(
                        "Warning: Organizer "
                                + organizer.getId()
                                + " has no department assigned, and no specific department provided"
                                + " for event. Cannot set department for venue reservation.");
                // Depending on your business logic, you might want to throw an error here
                // if a department is mandatory for a venue reservation.
            }

            VenueReservationDTO reservationRequestDTO =
                    new VenueReservationDTO(
                            null, // id - will be generated
                            savedEvent.getId(), // Link to the newly created event
                            savedEvent.getEventName(), // Use event name
                            mapUserToDTO(organizer), // Pass organizer DTO (or null if not needed by
                            // create)
                            departmentIdForReservation, // Use the determined department ID
                            departmentNameForReservation, // Use the determined department name
                            venue.getId(), // Venue ID
                            venue.getName(), // Venue Name
                            savedEvent.getStartTime(), // Use event start time
                            savedEvent.getEndTime(), // Use event end time
                            Status.PENDING.name(), // Initial status for reservation
                            null, // approvals - initially empty
                            null, // createdAt - will be generated
                            null // updatedAt - will be generated
                            );

            VenueReservationDTO createdReservation =
                    venueReservationService.createVenueReservation(reservationRequestDTO);

            System.out.println(
                    "Successfully created venue reservation ID: "
                            + createdReservation.id()
                            + " for event ID: "
                            + savedEvent.getId());

        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.err.println(
                    "Error automatically creating venue reservation for event "
                            + savedEvent.getId()
                            + ": "
                            + e.getMessage());
            throw new RuntimeException(
                    "Failed to create associated venue reservation: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println(
                    "Unexpected error automatically creating venue reservation for event "
                            + savedEvent.getId()
                            + ": "
                            + e.getMessage());
            throw new RuntimeException(
                    "Unexpected error creating associated venue reservation: " + e.getMessage(), e);
        }

        try {
            Venue eventVenue = savedEvent.getEventVenue();
            if (eventVenue != null && eventVenue.getVenueOwner() != null) {
                User venueOwner = eventVenue.getVenueOwner();
                if (venueOwner.getEmail() != null) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "VENUE_RESERVATION_REQUEST");
                    payload.put(
                            "message",
                            "New event '"
                                    + savedEvent.getEventName()
                                    + "' has been created and requires venue reservation"
                                    + " approval.");
                    payload.put("eventId", savedEvent.getId());
                    payload.put("relatedEntityType", "EVENT");
                    // Optionally add venueReservationId if available and relevant here
                    // payload.put("venueReservationId", createdReservation.id());
                    payload.put("eventName", savedEvent.getEventName());
                    payload.put("requester", savedEvent.getOrganizer().getFullName());
                    payload.put("venueName", eventVenue.getName());

                    notificationService.notifyUser(
                            venueOwner.getEmail(), "/queue/notifications", payload);
                }
            }

            User eventOrganizer = savedEvent.getOrganizer();
            if (eventOrganizer != null
                    && eventOrganizer.getDepartment() != null
                    && eventOrganizer.getDepartment().getDeptHead() != null) {
                User deptHead = eventOrganizer.getDepartment().getDeptHead();
                if (deptHead.getEmail() != null
                        && !deptHead.getId().equals(eventOrganizer.getId())) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "EVENT_APPROVAL_REQUEST");
                    payload.put(
                            "message",
                            "New event '"
                                    + savedEvent.getEventName()
                                    + "' by "
                                    + eventOrganizer.getFullName()
                                    + " has been created and requires your approval (Venue"
                                    + " reservation pending).");
                    payload.put("eventId", savedEvent.getId());
                    payload.put("relatedEntityType", "EVENT");
                    payload.put("eventName", savedEvent.getEventName());
                    payload.put("requester", eventOrganizer.getFullName());
                    payload.put("departmentName", eventOrganizer.getDepartment().getName());

                    notificationService.notifyUser(
                            deptHead.getEmail(), "/queue/notifications", payload);
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "Error sending notification for new event "
                            + savedEvent.getId()
                            + ": "
                            + e.getMessage());
        }

        return mapToDTO(savedEvent);
    }

    public List<EventDTO> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public EventDTO getEventById(Long eventId) {
        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with ID: " + eventId));
        return mapToDTO(event);
    }

    public List<EventDTO> getApprovedEvents() {
        List<Event> approvedEvents = eventRepository.findByStatus(Status.APPROVED);
        return approvedEvents.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public EventDTO updateEvent(
            Long eventId,
            EventDTO updatedEventDTO,
            MultipartFile approvedLetterFile,
            MultipartFile eventImageFile) {

        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with ID: " + eventId));

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
                                                "Authenticated user not found in"
                                                        + " database")); // Should
        // not
        // happen

        boolean isOrganizer =
                event.getOrganizer() != null
                        && event.getOrganizer().getId().equals(currentUser.getId());
        boolean isSuperAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;

        if (!isOrganizer && !isSuperAdmin) {
            throw new IllegalArgumentException("You are not authorized to update this event.");
        }

        Department newDepartment = event.getDepartment();
        if (updatedEventDTO.departmentId() != null) {
            if (event.getDepartment() == null
                    || !updatedEventDTO.departmentId().equals(event.getDepartment().getId())) {
                // Corrected part:
                Department fetchedDepartment =
                        departmentService.getDepartmentById(updatedEventDTO.departmentId());
                if (fetchedDepartment == null) {
                    throw new IllegalArgumentException(
                            "Department not found with ID: " + updatedEventDTO.departmentId());
                }
                newDepartment = fetchedDepartment;
            }
        } else {
            logger.warn(
                    "departmentId in updatedEventDTO is null for event {}. Department will not be"
                            + " changed unless it was already null.",
                    eventId);
        }

        Venue newVenue = event.getEventVenue();
        if (updatedEventDTO.eventVenueId() != null
                && (event.getEventVenue() == null
                        || !updatedEventDTO.eventVenueId().equals(event.getEventVenue().getId()))) {
            newVenue =
                    venueRepository
                            .findById(updatedEventDTO.eventVenueId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Venue not found with ID: "
                                                            + updatedEventDTO.eventVenueId()));
        }

        LocalDateTime newStartTime =
                updatedEventDTO.startTime() != null
                        ? updatedEventDTO.startTime()
                        : event.getStartTime();
        LocalDateTime newEndTime =
                updatedEventDTO.endTime() != null ? updatedEventDTO.endTime() : event.getEndTime();

        if (newVenue != event.getEventVenue()
                || newStartTime != event.getStartTime()
                || newEndTime != event.getEndTime()) {
            List<Event> conflictingEvents =
                    eventRepository.findConflictingEvents(
                            newVenue.getId(), newStartTime, newEndTime);

            boolean hasConflict =
                    conflictingEvents.stream()
                            .anyMatch(
                                    e ->
                                            !e.getId().equals(eventId)
                                                    && e.getStatus() != Status.CANCELED);

            if (hasConflict) {
                throw new IllegalArgumentException(
                        "There is a scheduling conflict with another event at this venue and"
                                + " time.");
            }
        }

        event.setEventName(
                updatedEventDTO.eventName() != null
                        ? updatedEventDTO.eventName()
                        : event.getEventName());
        event.setEventType(
                updatedEventDTO.eventType() != null
                        ? updatedEventDTO.eventType()
                        : event.getEventType());
        event.setDepartment(newDepartment);
        event.setStartTime(newStartTime);
        event.setEndTime(newEndTime);
        event.setEventVenue(newVenue);

        // Only allow SUPER_ADMIN to change status directly via update? Or handle via
        // separate approval flow?
        // For now, allowing status update if provided in DTO (consider restricting this
        // based on role if needed)
        // if (updatedEventDTO.status() != null) {
        // try {
        // Status newStatus =
        // Status.valueOf(updatedEventDTO.status().toUpperCase());
        // // Add logic here if status transitions need validation (e.g., cannot go
        // from
        // // CANCELED back to PENDING)
        // event.setStatus(newStatus);
        // } catch (IllegalArgumentException e) {
        // System.err.println(
        // "Invalid status provided during update: " +
        // updatedEventDTO.status());
        // // Optionally throw an exception or ignore invalid status
        // }
        // }
        // Note: Organizer should generally not be changed via update. If needed, create
        // a separate 'reassign' method.

        // --- File Updates ---
        if (approvedLetterFile != null && !approvedLetterFile.isEmpty()) {
            if (event.getApprovedLetterPath() != null && !event.getApprovedLetterPath().isBlank()) {
                fileStorageService.deleteFile(event.getApprovedLetterPath(), lettersBucketName);
            }
            String newLetterObjectName =
                    fileStorageService.uploadFile(
                            approvedLetterFile, lettersBucketName, "approved-letters/");
            event.setApprovedLetterPath(newLetterObjectName);
        }

        if (eventImageFile != null && !eventImageFile.isEmpty()) {
            if (event.getImagePath() != null && !event.getImagePath().isBlank()) {
                fileStorageService.deleteFile(event.getImagePath(), eventsBucketName);
            }
            String newImageObjectName =
                    fileStorageService.uploadFile(
                            eventImageFile, eventsBucketName, "event-images/");
            event.setImagePath(newImageObjectName);
        }

        Event savedEvent = eventRepository.save(event);
        return mapToDTO(savedEvent);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with ID: " + eventId));

        if (event.getApprovedLetterPath() != null && !event.getApprovedLetterPath().isBlank()) {
            fileStorageService.deleteFile(event.getApprovedLetterPath(), lettersBucketName);
        }

        if (event.getImagePath() != null && !event.getImagePath().isBlank()) {
            fileStorageService.deleteFile(event.getImagePath(), eventsBucketName);
        }

        // Consider related entities (like EventApproval) - should they be deleted?
        // If EventApproval has CascadeType.ALL or REMOVE on the 'event' relationship,
        // they might be deleted automatically. Otherwise, delete them manually if
        // required.
        // eventApprovalRepository.deleteAllByEvent(event); // Example if manual
        // deletion needed

        eventRepository.delete(event);
    }

    @Transactional
    public String cancelEvent(Long eventId) {
        Event event =
                eventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Event not found with ID: " + eventId));

        if (event.getStatus() == Status.CANCELED) {
            return "Event is already canceled.";
        }

        event.setStatus(Status.CANCELED);
        eventRepository.save(event);

        User canceller = getCurrentUser();
        User organizer = event.getOrganizer();

        if (organizer != null
                && organizer.getEmail() != null
                && !organizer.getId().equals(canceller.getId())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_CANCELED");
            payload.put(
                    "message",
                    "Your event '"
                            + event.getEventName()
                            + "' has been canceled by "
                            + canceller.getFullName()
                            + ".");
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("cancellerName", canceller.getFullName());
            notificationService.notifyUser(organizer.getEmail(), "/queue/notifications", payload);
        }

        Venue venue = event.getEventVenue();
        if (venue != null
                && venue.getVenueOwner() != null
                && venue.getVenueOwner().getEmail() != null
                && !venue.getVenueOwner().getId().equals(canceller.getId())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "EVENT_CANCELED_INFO"); // Different type for info
            payload.put(
                    "message",
                    "Event '"
                            + event.getEventName()
                            + "' scheduled at your venue '"
                            + venue.getName()
                            + "' has been canceled by "
                            + canceller.getFullName()
                            + ".");
            payload.put("eventId", event.getId());
            payload.put("relatedEntityType", "EVENT");
            payload.put("eventName", event.getEventName());
            payload.put("venueName", venue.getName());
            payload.put("cancellerName", canceller.getFullName());
            notificationService.notifyUser(
                    venue.getVenueOwner().getEmail(), "/queue/notifications", payload);
        }

        if (organizer != null
                && organizer.getDepartment() != null
                && organizer.getDepartment().getDeptHead() != null) {
            User deptHead = organizer.getDepartment().getDeptHead();
            if (deptHead != null
                    && deptHead.getEmail() != null
                    && !deptHead.getId().equals(canceller.getId())) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "EVENT_CANCELED_INFO");
                payload.put(
                        "message",
                        "Event '"
                                + event.getEventName()
                                + "' organized by "
                                + organizer.getFullName()
                                + " from your department has been canceled by "
                                + canceller.getFullName()
                                + ".");
                payload.put("eventId", event.getId());
                payload.put("relatedEntityType", "EVENT");
                payload.put("eventName", event.getEventName());
                payload.put("organizerName", organizer.getFullName());
                payload.put("cancellerName", canceller.getFullName());
                notificationService.notifyUser(
                        deptHead.getEmail(), "/queue/notifications", payload);
            }
        }
        // Add notifications for other relevant roles (equipment owners, etc.) if needed

        return "Event canceled successfully";
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

    public List<EventDTO> getPendingEventsForVenueOwner() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.VENUE_OWNER.toString())) {
            return Collections.emptyList();
        }
        List<Event> events = eventRepository.findPendingEventsForVenueOwner(currentUser);
        return events.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<EventDTO> getPendingEventsForDeptHead() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRoles().toString().contains(Role.DEPT_HEAD.toString())) {
            return Collections.emptyList();
        }
        List<Event> events = eventRepository.findPendingEventsForDeptHead(currentUser);
        return events.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public EventDTO mapToDTO(Event event) {
        String letterUrl = null;
        if (event.getApprovedLetterPath() != null && !event.getApprovedLetterPath().isBlank()) {
            letterUrl =
                    fileStorageService.getFileUrl(event.getApprovedLetterPath(), lettersBucketName);
        }

        String imageUrl = null;
        if (event.getImagePath() != null && !event.getImagePath().isBlank()) {
            imageUrl = fileStorageService.getFileUrl(event.getImagePath(), eventsBucketName);
        }

        UserDTO organizerDto = mapUserToDTO(event.getOrganizer());

        Long departmentId = null;
        if (event.getDepartment() != null) {
            departmentId = event.getDepartment().getId();
        }

        return new EventDTO(
                event.getId(),
                event.getEventName(),
                event.getEventType(),
                organizerDto,
                event.getEventVenue() != null ? event.getEventVenue().getId() : null,
                departmentId,
                event.getStartTime(),
                event.getEndTime(),
                event.getStatus() != null ? event.getStatus().toString() : null,
                letterUrl,
                imageUrl,
                event.getCreatedAt(),
                event.getUpdatedAt());
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
}
