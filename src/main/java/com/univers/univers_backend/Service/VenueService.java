package com.univers.univers_backend.Service;

import java.util.List;
import java.util.NoSuchElementException; 
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

            ownerDto = mapUserToDTO(venueOwner); 
        }
        Venue savedVenue = venueRepository.save(newVenue);
        
        if (savedVenue.getVenueOwner() != null && ownerDto == null) {
             User savedOwner = savedVenue.getVenueOwner();
             ownerDto = mapUserToDTO(savedOwner); 
        }

        return mapVenueToDTO(savedVenue, ownerDto); 
    }

    public List<VenueDTO> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();

        return venues.stream()
                .map(venue -> {
                    UserDTO ownerDto = null;
                    if (venue.getVenueOwner() != null) {
                        ownerDto = mapUserToDTO(venue.getVenueOwner()); 
                    }
                    return mapVenueToDTO(venue, ownerDto);
                })
                .collect(Collectors.toList());

     }

    public VenueDTO getVenueById(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new NoSuchElementException("Venue not found with ID: " + venueId));
        
        UserDTO ownerDto = null;
        if (venue.getVenueOwner() != null) {
            ownerDto = mapUserToDTO(venue.getVenueOwner());
        }
        return mapVenueToDTO(venue, ownerDto);
    }

    public VenueDTO updateVenue(Long venueId, VenueDTO venueDTO) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new NoSuchElementException("Venue not found with ID: " + venueId));

        if (venueDTO.name() != null && !venueDTO.name().equalsIgnoreCase(venue.getName())) {
            Optional<Venue> existingVenueWithName = venueRepository.findByNameIgnoreCase(venueDTO.name());
            if (existingVenueWithName.isPresent()) {
                throw new IllegalArgumentException("Another venue with the name '" + venueDTO.name() + "' already exists.");
            }
            venue.setName(venueDTO.name());
        }

        if (venueDTO.location() != null) {
            venue.setLocation(venueDTO.location());
        }

        UserDTO ownerDto = null;
        if (venueDTO.venueOwner() != null && venueDTO.venueOwner().id() != null) {
            if (venue.getVenueOwner() == null || !venue.getVenueOwner().getId().equals(venueDTO.venueOwner().id())) {
                User newVenueOwner = userRepository.findById(venueDTO.venueOwner().id())
                        .orElseThrow(() -> new IllegalArgumentException("User (Venue Owner) not found with ID: " + venueDTO.venueOwner().id()));
                venue.setVenueOwner(newVenueOwner);
                ownerDto = mapUserToDTO(newVenueOwner);
            } else {
                 ownerDto = mapUserToDTO(venue.getVenueOwner());
            }
        } else if (venueDTO.venueOwner() == null && venue.getVenueOwner() != null) {
             venue.setVenueOwner(null);
             ownerDto = null;
        } else if (venue.getVenueOwner() != null) {
             ownerDto = mapUserToDTO(venue.getVenueOwner());
        }

        Venue updatedVenue = venueRepository.save(venue);
        return mapVenueToDTO(updatedVenue, ownerDto); 
    }

    public void deleteVenue(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new NoSuchElementException("Venue not found with ID: " + venueId);
        }
        // Consider adding checks here if the venue is associated with events before deleting
        venueRepository.deleteById(venueId);
    }

    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles().name(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private VenueDTO mapVenueToDTO(Venue venue, UserDTO ownerDto) {
         return new VenueDTO(
                venue.getId(),
                venue.getName(),
                venue.getLocation(),
                ownerDto,
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }
}