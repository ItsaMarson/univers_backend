package com.univers.univers_backend.Service;


import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Repository.EventRepository;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository eventRepository;


    public EventService(EventRepository eventRepository){
        this.eventRepository = eventRepository;
    }

    public String createEvent(EventDTO eventDTO){

        if(eventRepository.existsById(eventDTO.id())){
            return "Event already exists";
        }
        Event event = new Event();
        event.setEventName(eventDTO.eventName());
        event.setEventType(eventDTO.eventType());
        event.setStartTime(eventDTO.startTime());
        event.setEndTime(eventDTO.endTime());
        event.setApprovedLetter(eventDTO.approvedLetter());

        eventRepository.save(event);
        return "Event created successfully!";
    }
}
