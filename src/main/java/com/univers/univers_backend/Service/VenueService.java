package com.univers.univers_backend.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;


    public VenueService(VenueRepository venueRepository, UserRepository userRepository){
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }
    public VenueDTO addVenue(VenueDTO venueDTO) {
        Optional<Venue> existingVenue = venueRepository.findByNameIgnoreCase(venueDTO.name());

        if(existingVenue.isPresent()){
            throw new IllegalArgumentException("Venue already exists.");
        }
        Venue newVenue = new Venue();
        newVenue.setName(venueDTO.name());
        newVenue.setLocation(venueDTO.location());

        UserDTO ownerDto = null; 
        if(venueDTO.venueOwner() != null && venueDTO.venueOwner().id() != null){ 
            User venueOwner = userRepository.findById(venueDTO.venueOwner().id())
                    .orElseThrow(() -> new IllegalArgumentException("User (Venue Owner) not found"));
            
            newVenue.setVenueOwner(venueOwner);

            ownerDto = new UserDTO(
                    venueOwner.getId(),
                    venueOwner.getEmail(),
                    venueOwner.getFirstname(),
                    venueOwner.getLastname(),
                    venueOwner.getId_number(),
                    venueOwner.getPhone_number(),
                    venueOwner.getTelephoneNumber(),
                    venueOwner.getRoles().name(),
                    venueOwner.getDepartment() != null ? venueOwner.getDepartment().getId() : null,
                    venueOwner.getEmailVerified(),
                    venueOwner.isActive(),
                    venueOwner.getCreatedAt(),
                    venueOwner.getUpdatedAt()
            );
        }
        Venue savedVenue = venueRepository.save(newVenue);
        
        if (savedVenue.getVenueOwner() != null && ownerDto == null) {
             User savedOwner = savedVenue.getVenueOwner();
             ownerDto = new UserDTO(
                    savedOwner.getId(),
                    savedOwner.getEmail(),
                    savedOwner.getFirstname(),
                    savedOwner.getLastname(),
                    savedOwner.getId_number(),
                    savedOwner.getPhone_number(),
                    savedOwner.getTelephoneNumber(),
                    savedOwner.getRoles().name(),
                    savedOwner.getDepartment() != null ? savedOwner.getDepartment().getId() : null,
                    savedOwner.getEmailVerified(),
                    savedOwner.isActive(),
                    savedOwner.getCreatedAt(),
                    savedOwner.getUpdatedAt()
            );
        }

        return new VenueDTO(
                savedVenue.getId(),
                savedVenue.getName(),
                savedVenue.getLocation(),
                ownerDto, 
                savedVenue.getCreatedAt(),
                savedVenue.getUpdatedAt()
        );
    }

    public List<VenueDTO> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();

        return venues.stream()
                .map(venue -> {
                    UserDTO ownerDto = null;
                    if (venue.getVenueOwner() != null) {
                        User owner = venue.getVenueOwner();
                        ownerDto = new UserDTO(
                                owner.getId(),
                                owner.getEmail(),
                                owner.getFirstname(),
                                owner.getLastname(),
                                owner.getId_number(),
                                owner.getPhone_number(),
                                owner.getTelephoneNumber(),
                                owner.getRoles().name(),
                                owner.getDepartment() != null ? owner.getDepartment().getId() : null,
                                owner.getEmailVerified(),
                                owner.isActive(),
                                owner.getCreatedAt(),
                                owner.getUpdatedAt()
                        );
                    }
                    return new VenueDTO(
                            venue.getId(),
                            venue.getName(),
                            venue.getLocation(),
                            ownerDto,
                            venue.getCreatedAt(),
                            venue.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());

     }
}