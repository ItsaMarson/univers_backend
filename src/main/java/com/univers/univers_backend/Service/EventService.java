/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.CreateEventRequestDTO;
import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UpdateEventRequestDTO;
import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.*;
import com.univers.univers_backend.Repository.EventApprovalRepository;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import java.util.List;
import java.util.NoSuchElementException;
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
    private final UserMapper userMapper;
    private final VenueMapper venueMapper;
    private final DepartmentMapper departmentMapper;

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
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
        this.departmentMapper = departmentMapper;
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

    public List<EventDTO> getApprovedEvents() {
        List<Event> events = eventRepository.findByStatus(Status.APPROVED);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
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
        boolean isAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;
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

        if (currentUser.getRoles() == Role.SUPER_ADMIN) {
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

        boolean isAdmin = currentUser.getRoles() == Role.SUPER_ADMIN;
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

    public List<EventDTO> getPendingEventsForVenueOwner() {
        User venueOwner = getCurrentUser();
        List<Event> events = eventRepository.findPendingEventsForVenueOwner(venueOwner);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public List<EventDTO> getPendingEventsForDeptHead() {
        User deptHead = getCurrentUser();
        List<Event> events = eventRepository.findPendingEventsForDeptHead(deptHead);
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    // Added public method to allow UserService to map Event to EventDTO via EventService
    public EventDTO mapToDTO(Event event) {
        if (event == null) {
            return null;
        }
        return this.eventMapper.toDto(event);
    }
}
