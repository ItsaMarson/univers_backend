package com.univers.univers_backend.Controller;


import com.univers.univers_backend.Service.EventApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event-approval")
public class EventApprovalController {

    private final EventApprovalService eventApprovalService;

    public EventApprovalController(EventApprovalService eventApprovalService){
        this.eventApprovalService = eventApprovalService;
    }

    @PatchMapping("/approve")
    public ResponseEntity<String> approveEvent(@RequestParam Long eventId,
                                               @RequestParam Long userId,
                                               @RequestParam String remarks){
        String response = eventApprovalService.approveEvent(eventId, userId, remarks);
        if(response.startsWith("Error")){
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
