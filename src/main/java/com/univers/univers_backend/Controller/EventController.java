package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {


    private final EventService eventService;

    public EventController(EventService eventService){
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestPart("event") EventDTO eventDTO,
                                              @RequestPart(value = "approvedLetter") MultipartFile approvedLetter){

        try{
            EventDTO event = eventService.createEvent(eventDTO, approvedLetter);
            return new ResponseEntity<>(event, HttpStatus.CREATED);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        }
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents(){
        List<EventDTO> allEvents = eventService.getAllEvents();
        return ResponseEntity.ok(allEvents);
    }
    @PatchMapping("/{eventId}")
    public ResponseEntity<String> updateEvent(@PathVariable Long eventId,
                                              @RequestPart("event") EventDTO updatedEvent,
                                              @RequestPart(value = "approvedLetter", required = false) MultipartFile approvedLetter){
        String responseMessage = eventService.updateEvent(eventId, updatedEvent, approvedLetter);
        if (responseMessage.startsWith("Event does not exist") ||
                responseMessage.startsWith("You are not authorized") ||
                responseMessage.startsWith("Venue does not exist")) {
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }
    @PatchMapping("/{eventId}/cancel")
    public ResponseEntity<String> cancelEvent(@PathVariable Long eventId){
        String responseMessage = eventService.cancelEvent(eventId);
        if(responseMessage.startsWith("Event does not exist. Invalid event Id")){
            return ResponseEntity.badRequest().body(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }
}
