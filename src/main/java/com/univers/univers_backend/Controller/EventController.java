package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {


    private final EventService eventService;

    public EventController(EventService eventService){
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<String> createEvent(@RequestBody EventDTO eventDTO){
        String responseMessage = eventService.createEvent(eventDTO);

        if("Organizer not found".equals(responseMessage) || "There is a scheduling conflict with another event.".equals(responseMessage)){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }
}
