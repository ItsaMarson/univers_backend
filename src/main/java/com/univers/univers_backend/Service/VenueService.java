package com.univers.univers_backend.Service;

import java.io.IOException; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.nio.file.Paths; 
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;

import jakarta.transaction.Transactional;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Value("${upload.venue.dir}") 
    private String uploadDir;


    public VenueService(VenueRepository venueRepository, UserRepository userRepository){
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
    }

    @Transactional // Added Transactional
    public VenueDTO addVenue(VenueDTO venueDTO, MultipartFile imageFile) {
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

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            newVenue.setImagePath(imagePath);
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

    @Transactional
    public VenueDTO updateVenue(Long venueId, VenueDTO venueDTO, MultipartFile imageFile) { // Added imageFile parameter
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

        if (imageFile != null && !imageFile.isEmpty()) {
            deleteImage(venue.getImagePath()); // Delete old image if it exists
            String newImagePath = saveImage(imageFile);
            venue.setImagePath(newImagePath);
        }

        Venue updatedVenue = venueRepository.save(venue);
        if (updatedVenue.getVenueOwner() != null && ownerDto == null) {
             ownerDto = mapUserToDTO(updatedVenue.getVenueOwner());
        }
        return mapVenueToDTO(updatedVenue, ownerDto);
    }

    @Transactional 
    public void deleteVenue(Long venueId) {
        Venue venue = venueRepository.findById(venueId) 
            .orElseThrow(() -> new NoSuchElementException("Venue not found with ID: " + venueId));

        deleteImage(venue.getImagePath());

        venueRepository.deleteById(venueId);
    }


    private String saveImage(MultipartFile imageFile) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename == null) {
                 throw new RuntimeException("Image file name is null.");
            }
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            String uniqueFilename = System.currentTimeMillis() + "_" + sanitizedFilename;

            Path filePath = uploadPath.resolve(uniqueFilename);
            imageFile.transferTo(filePath.toFile());

            // Return the relative path or just the filename if preferred
            // For consistency with EquipmentService, returning full path for now
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save venue image file. Please try again.", e);
        }
    }

    private void deleteImage(String imagePathString) {
        if (imagePathString != null && !imagePathString.isEmpty()) {
            try {
                 Path imagePath = Paths.get(imagePathString);
                 Files.deleteIfExists(imagePath);
             } catch (IOException e) {
                 // Log the error but don't stop the main operation (e.g., venue deletion)
                 System.err.println("Failed to delete venue image file: " + imagePathString + ". Error: " + e.getMessage());
                 // logger.error("Failed to delete image file: {}", imagePathString, e);
             }
        }
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
                user.getRoles() != null ? user.getRoles().name() : null,
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
                venue.getImagePath(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }
}