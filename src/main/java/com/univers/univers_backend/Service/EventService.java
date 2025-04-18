package com.univers.univers_backend.Service;


import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    @Value("${upload.letter.dir}")
    private String uploadDir;


    public EventService(EventRepository eventRepository, UserRepository userRepository, VenueRepository venueRepository){
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    public EventDTO createEvent(EventDTO eventDTO, MultipartFile approvedLetter){

        User organizer = userRepository.findById(eventDTO.organizerId())
                .orElseThrow(()-> new IllegalArgumentException("Organizer not found"));

        List<Event> conflictingEvents = eventRepository.findConflictingEvents(eventDTO.eventVenueId(),
                eventDTO.startTime(), eventDTO.endTime());

        if (!conflictingEvents.isEmpty()) {
            throw new IllegalArgumentException("There is a scheduling conflict with another event.");
        }
        Venue venue = venueRepository.findById(eventDTO.eventVenueId())
                .orElseThrow(()-> new IllegalArgumentException("Invalid venue ID"));

        Event event = new Event();
        event.setEventName(eventDTO.eventName());
        event.setEventType(eventDTO.eventType());
        event.setStartTime(eventDTO.startTime());
        event.setEndTime(eventDTO.endTime());
        event.setOrganizer(organizer);
        event.setEventVenue(venue);
        event.setStatus(Status.PENDING);

        if(approvedLetter != null && !approvedLetter.isEmpty()){
            try{
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                if(approvedLetter.getOriginalFilename() != null){
                    Path filePath = uploadPath.resolve(approvedLetter.getOriginalFilename());
                    approvedLetter.transferTo(filePath.toFile());

                    event.setApprovedLetterPath(filePath.toString());
                }

            }catch (IOException e){
                throw new RuntimeException("Failed to upload file. Please try again");
            }
        }

        Event savedEvent = eventRepository.save(event);

        return new EventDTO(
                savedEvent.getId(),
                savedEvent.getEventName(),
                savedEvent.getEventType(),
                savedEvent.getOrganizer().getId(),
                savedEvent.getApprovedLetterPath(),
                savedEvent.getEventVenue().getId(),
                savedEvent.getStartTime(),
                savedEvent.getEndTime(),
                savedEvent.getStatus().toString()
        );

    }

    public List<EventDTO> getAllEvents(){
        List<Event> events = eventRepository.findAll();

        List<EventDTO> eventDTOList = new ArrayList<>();
        for(Event event: events){
            EventDTO eventDTO = new EventDTO(
                    event.getId(),
                    event.getEventName(),
                    event.getEventType(),
                    event.getOrganizer().getId(),
                    event.getApprovedLetterPath(),
                    event.getEventVenue().getId(),
                    event.getStartTime(),
                    event.getEndTime(),
                    event.getStatus().toString()
            );
            eventDTOList.add(eventDTO);
        }
        return eventDTOList;
    }

    public String updateEvent(Long eventId, EventDTO updatedEvent, MultipartFile approvedLetter){

        Optional<Event> existingEvent = eventRepository.findById(eventId);
        Optional<Venue> venueOptional = venueRepository.findById(updatedEvent.eventVenueId());

        if(existingEvent.isEmpty()){
             return "Event does not exist. Invalid event id";
         }
        Event event = existingEvent.get();

        if (event.getOrganizer() == null || !event.getOrganizer().getId().equals(updatedEvent.organizerId())) {
            return "You are not authorized to update this event.";
        }

        if(venueOptional.isEmpty()){
            return "Venue does not exist";
        }
        Venue newVenue = venueOptional.get();
        LocalDateTime newStartTime = updatedEvent.startTime() != null ? updatedEvent.startTime() : event.getStartTime();
        LocalDateTime newEndTime = updatedEvent.endTime() != null ? updatedEvent.endTime() : event.getEndTime();

        List<Event> conflictingEvents = eventRepository.findConflictingEvents(
                updatedEvent.eventVenueId(), newStartTime, newEndTime);

        boolean hasConflict = conflictingEvents.stream()
                .anyMatch(e -> !e.getId().equals(eventId) && e.getStatus() != Status.CANCELED);

        if (hasConflict) {
            return "There is a scheduling conflict with another event at this venue.";
        }

        event.setEventName(updatedEvent.eventName() != null ? updatedEvent.eventName() : event.getEventName());
        event.setEventType(updatedEvent.eventType() != null ? updatedEvent.eventType() : event.getEventType());
        event.setStartTime(newStartTime);
        event.setEndTime(newEndTime);
        event.setEventVenue(newVenue);

        if(approvedLetter != null && !approvedLetter.isEmpty()){
            try{
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                if(approvedLetter.getOriginalFilename() != null){
                    Path filePath = uploadPath.resolve(approvedLetter.getOriginalFilename());
                    approvedLetter.transferTo(filePath.toFile());

                    event.setApprovedLetterPath(filePath.toString());
                }

            }catch (IOException e){
                return "Failed to upload file. Please try again";
            }
        }
        eventRepository.save(event);
        return "Event updated successfully";
    }

    public String cancelEvent(Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if(eventOptional.isEmpty()){
            return "Event does not exist. Invalid event Id";
        }
        Event event = eventOptional.get();
        event.setStatus(Status.CANCELED);
        eventRepository.save(event);

        return "Event canceled successfully";
    }
}
