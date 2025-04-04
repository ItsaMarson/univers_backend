package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;


    public VenueService(VenueRepository venueRepository, UserRepository userRepository){
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }
    public String addVenue(VenueDTO venueDTO) {
        Optional<Venue> existingVenue = venueRepository.findByNameIgnoreCase(venueDTO.name());

        if(existingVenue.isPresent()){
            return "Venue already exists.";
        }
        Venue newVenue = new Venue();
        newVenue.setName(venueDTO.name());
        newVenue.setLocation(venueDTO.location());

        if(venueDTO.venueOwnerId() != null){
            User venueOwner = userRepository.findById(venueDTO.venueOwnerId()).orElse(null);

            if(venueOwner == null){
                return "User not found.";
            }
            newVenue.setVenueOwner(venueOwner);
        }
        venueRepository.save(newVenue);
        return "Venue is added successfully";
    }

    public List<VenueDTO> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();

        return venues.stream()
                .map(venue -> new VenueDTO(
                        venue.getName(),
                        venue.getLocation(),
                        venue.getVenueOwner() != null ? venue.getVenueOwner().getId() : null,
                        venue.getCreatedAt(),
                        venue.getUpdatedAt()
                ))
                .collect(Collectors.toList());

     }
}
