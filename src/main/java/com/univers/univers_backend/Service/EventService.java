/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors; // Import Collectors
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final FileStorageService fileStorageService;

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
            FileStorageService fileStorageService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.fileStorageService = fileStorageService;
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

        List<Event> conflictingEvents =
                eventRepository.findConflictingEvents(
                        venue.getId(), eventDTO.startTime(), eventDTO.endTime());
        if (!conflictingEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "There is a scheduling conflict with another event at this venue and time.");
        }

        Event event = new Event();
        event.setEventName(eventDTO.eventName());
        event.setEventType(eventDTO.eventType());
        event.setStartTime(eventDTO.startTime());
        event.setEndTime(eventDTO.endTime());
        event.setOrganizer(organizer);
        event.setEventVenue(venue);
        event.setStatus(Status.PENDING);

        if (approvedLetterFile != null && !approvedLetterFile.isEmpty()) {
            String letterObjectName =
                    fileStorageService.uploadFile(
                            approvedLetterFile, lettersBucketName, "approved-letters/");
            event.setApprovedLetterPath(letterObjectName); // Store object name
        }

        if (eventImageFile != null && !eventImageFile.isEmpty()) {
            String imageObjectName =
                    fileStorageService.uploadFile(
                            eventImageFile, eventsBucketName, "event-images/");
            event.setImagePath(imageObjectName);
        }

        Event savedEvent = eventRepository.save(event);
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

        if (event.getOrganizer() == null
                || updatedEventDTO.organizer() == null
                || !event.getOrganizer().getId().equals(updatedEventDTO.organizer().id())) {
            throw new IllegalArgumentException("You are not authorized to update this event.");
        }

        Venue newVenue =
                venueRepository
                        .findById(updatedEventDTO.eventVenueId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Venue not found with ID: "
                                                        + updatedEventDTO.eventVenueId()));

        LocalDateTime newStartTime =
                updatedEventDTO.startTime() != null
                        ? updatedEventDTO.startTime()
                        : event.getStartTime();
        LocalDateTime newEndTime =
                updatedEventDTO.endTime() != null ? updatedEventDTO.endTime() : event.getEndTime();

        List<Event> conflictingEvents =
                eventRepository.findConflictingEvents(newVenue.getId(), newStartTime, newEndTime);

        boolean hasConflict =
                conflictingEvents.stream()
                        .anyMatch(
                                e ->
                                        !e.getId().equals(eventId)
                                                && e.getStatus() != Status.CANCELED);

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "There is a scheduling conflict with another event at this venue and time.");
        }

        event.setEventName(
                updatedEventDTO.eventName() != null
                        ? updatedEventDTO.eventName()
                        : event.getEventName());
        event.setEventType(
                updatedEventDTO.eventType() != null
                        ? updatedEventDTO.eventType()
                        : event.getEventType());
        event.setStartTime(newStartTime);
        event.setEndTime(newEndTime);
        event.setEventVenue(newVenue);
        if (updatedEventDTO.status() != null) {
            try {
                event.setStatus(Status.valueOf(updatedEventDTO.status().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid status provided: " + updatedEventDTO.status());
            }
        }

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

        event.setStatus(Status.CANCELED);
        eventRepository.save(event);

        return "Event canceled successfully";
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

        UserDTO organizerDto = mapUserToDTO(event.getOrganizer()); // Use helper

        return new EventDTO(
                event.getId(),
                event.getEventName(),
                event.getEventType(),
                organizerDto,
                event.getEventVenue() != null ? event.getEventVenue().getId() : null,
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
