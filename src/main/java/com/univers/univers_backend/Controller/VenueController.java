package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService){
        this.venueService = venueService;
    }

}
