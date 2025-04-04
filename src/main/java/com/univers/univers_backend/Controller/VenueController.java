package com.univers.univers_backend.Controller;


import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Repository.VenueRepository;
import com.univers.univers_backend.Service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
public class VenueController {

    private final VenueService venueService;
    private final VenueRepository venueRepository;
    public VenueController(VenueService venueService, VenueRepository venueRepository){
        this.venueService = venueService;
        this.venueRepository = venueRepository;
    }
    @GetMapping
    public ResponseEntity<List<VenueDTO>> getAllVenues(){
        List<VenueDTO> venues = venueService.getAllVenues();
        if(venues.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(venues);
    }
}
