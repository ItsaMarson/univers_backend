package com.univers.univers_backend.Service;


import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.EventRepository;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;


    public EventService(EventRepository eventRepository, UserRepository userRepository, VenueRepository venueRepository){
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
    }

    public String createEvent(EventDTO eventDTO){

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
        event.setApprovedLetter(eventDTO.approvedLetter());
        event.setOrganizer(organizerOptional.get());

        Optional<Venue> venue = venueRepository.findById(eventDTO.eventVenueId());
        if(venue.isEmpty()){
            return "Invalid venue Id";
        }

        event.setEventVenue(venue.get());

        eventRepository.save(event);
        return "Event created successfully!";
    }
}
