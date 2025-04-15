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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public String createEvent(EventDTO eventDTO, MultipartFile approvedLetter){

        Optional<User> organizerOptional = userRepository.findById(eventDTO.organizerId());
        if(organizerOptional.isEmpty()){
            return "Organizer not found";
        }
        List<Event> conflictingEvents = eventRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                eventDTO.endTime(), eventDTO.startTime());

        if (!conflictingEvents.isEmpty()) {
            return "There is a scheduling conflict with another event.";
        }

        Event event = new Event();
        event.setEventName(eventDTO.eventName());
        event.setEventType(eventDTO.eventType());
        event.setStartTime(eventDTO.startTime());
        event.setEndTime(eventDTO.endTime());
        event.setOrganizer(organizerOptional.get());
        event.setStatus(Status.PENDING);
        Optional<Venue> venue = venueRepository.findById(eventDTO.eventVenueId());
        if(venue.isEmpty()){
            return "Invalid venue Id";
        }
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
        event.setEventVenue(venue.get());

        eventRepository.save(event);
        return "Event created successfully!";
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
                    event.getEventVenue().getId(),
                    event.getStartTime(),
                    event.getEndTime()
            );
            eventDTOList.add(eventDTO);
        }
        return eventDTOList;
    }
}
