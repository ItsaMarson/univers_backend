package com.univers.univers_backend.Controller;


import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.Service.EventService;

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

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable Long eventId) {
        try {
            EventDTO event = eventService.getEventById(eventId);
            return ResponseEntity.ok(event);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong.");
        }
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
