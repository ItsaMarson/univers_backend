/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService; // Inject FileStorageService

    // Remove @Value for uploadDir
    // @Value("${upload.venue.dir}")
    // private String uploadDir;

    @Value("${minio.bucket.venues}") // Inject MinIO bucket name
    private String venuesBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    // Update constructor
    public VenueService(
            VenueRepository venueRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService; // Add to constructor
    }

    @Transactional // Added Transactional
    public VenueDTO addVenue(VenueDTO venueDTO, MultipartFile imageFile) {
        Optional<Venue> existingVenue = venueRepository.findByNameIgnoreCase(venueDTO.name());

        if (existingVenue.isPresent()) {
            throw new IllegalArgumentException("Venue already exists.");
        }
        Venue newVenue = new Venue();
        newVenue.setName(venueDTO.name());
        newVenue.setLocation(venueDTO.location());

        UserDTO ownerDto = null;
        if (venueDTO.venueOwner() != null && venueDTO.venueOwner().id() != null) {
            User venueOwner =
                    userRepository
                            .findById(venueDTO.venueOwner().id())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "User (Venue Owner) not found"));

            newVenue.setVenueOwner(venueOwner);
            ownerDto = mapUserToDTO(venueOwner);
        }

        // Use FileStorageService for upload
        if (imageFile != null && !imageFile.isEmpty()) {
            String objectName =
                    fileStorageService.uploadFile(
                            imageFile, venuesBucketName, "venue-images/"); // Store object name
            newVenue.setImagePath(objectName); // Store the MinIO object name
        }

        Venue savedVenue = venueRepository.save(newVenue);

        // Regenerate ownerDto if it wasn't set initially but owner exists after save
        if (savedVenue.getVenueOwner() != null && ownerDto == null) {
            User savedOwner = savedVenue.getVenueOwner();
            ownerDto = mapUserToDTO(savedOwner);
        }

        return mapVenueToDTO(savedVenue, ownerDto); // mapVenueToDTO needs update
    }

    public List<VenueDTO> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();
        return venues.stream()
                .map(
                        venue ->
                                mapVenueToDTO(
                                        venue,
                                        mapUserToDTO(venue.getVenueOwner()))) // mapVenueToDTO needs
                // update
                .collect(Collectors.toList());
    }

    public VenueDTO getVenueById(Long venueId) {
        Venue venue =
                venueRepository
                        .findById(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with ID: " + venueId));
        return mapVenueToDTO(
                venue, mapUserToDTO(venue.getVenueOwner())); // mapVenueToDTO needs update
    }

    @Transactional
    public VenueDTO updateVenue(Long venueId, VenueDTO venueDTO, MultipartFile imageFile) {
        Venue venue =
                venueRepository
                        .findById(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with ID: " + venueId));

        // Check for name conflict only if name is changing
        if (venueDTO.name() != null
                && !venueDTO.name().isBlank()
                && !venueDTO.name().equalsIgnoreCase(venue.getName())) {
            Optional<Venue> existingVenueWithName =
                    venueRepository.findByNameIgnoreCase(venueDTO.name());
            if (existingVenueWithName.isPresent()
                    && !existingVenueWithName.get().getId().equals(venueId)) {
                throw new IllegalArgumentException(
                        "Another venue with the name '" + venueDTO.name() + "' already exists.");
            }
            venue.setName(venueDTO.name());
        }

        if (venueDTO.location() != null && !venueDTO.location().isBlank()) {
            venue.setLocation(venueDTO.location());
        }

        UserDTO ownerDto = null;
        // Handle owner update
        if (venueDTO.venueOwner() != null && venueDTO.venueOwner().id() != null) {
            if (venue.getVenueOwner() == null
                    || !venue.getVenueOwner().getId().equals(venueDTO.venueOwner().id())) {
                User newVenueOwner =
                        userRepository
                                .findById(venueDTO.venueOwner().id())
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "User (Venue Owner) not found with ID: "
                                                                + venueDTO.venueOwner().id()));
                venue.setVenueOwner(newVenueOwner);
                ownerDto = mapUserToDTO(newVenueOwner);
            } else {
                // Owner hasn't changed, map existing one
                ownerDto = mapUserToDTO(venue.getVenueOwner());
            }
        } else if (venueDTO.venueOwner() == null && venue.getVenueOwner() != null) {
            // Owner is being removed
            venue.setVenueOwner(null);
            ownerDto = null;
        } else if (venue.getVenueOwner() != null) {
            // Owner exists and wasn't changed in DTO, map existing one
            ownerDto = mapUserToDTO(venue.getVenueOwner());
        }

        // Handle image update
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old image from MinIO if it exists
            if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
                fileStorageService.deleteFile(venue.getImagePath(), venuesBucketName);
            }
            // Upload new image
            String newObjectName =
                    fileStorageService.uploadFile(imageFile, venuesBucketName, "venue-images/");
            venue.setImagePath(newObjectName); // Store the new object name
        }

        Venue updatedVenue = venueRepository.save(venue);

        // Ensure ownerDto is correctly set after potential updates
        if (updatedVenue.getVenueOwner() != null && ownerDto == null) {
            ownerDto = mapUserToDTO(updatedVenue.getVenueOwner());
        } else if (updatedVenue.getVenueOwner() == null) {
            ownerDto = null;
        }

        return mapVenueToDTO(updatedVenue, ownerDto); // mapVenueToDTO needs update
    }

    @Transactional
    public void deleteVenue(Long venueId) {
        Venue venue =
                venueRepository
                        .findById(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with ID: " + venueId));

        // Delete image from MinIO if it exists
        if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
            fileStorageService.deleteFile(venue.getImagePath(), venuesBucketName);
        }

        // Now delete the venue record
        venueRepository.deleteById(venueId);
    }

    // Remove saveImage and deleteImage methods
    /*
     * private String saveImage(MultipartFile imageFile) { ... }
     * private void deleteImage(String imagePathString) { ... }
     */

    // This mapping needs to stay
    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isBlank()) {
            try {
                profileImageUrl =
                        fileStorageService.getFileUrl(user.getProfileImagePath(), usersBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for user "
                                + user.getId()
                                + ": "
                                + e.getMessage());
            }
        }
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstname() != null ? user.getFirstname() : null,
                user.getLastname() != null ? user.getLastname() : null,
                user.getId_number() != null ? user.getId_number() : null,
                user.getPhone_number() != null ? user.getPhone_number() : null,
                user.getTelephoneNumber() != null ? user.getTelephoneNumber() : null,
                user.getRoles() != null ? user.getRoles().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // Update mapVenueToDTO to generate URL from object name
    private VenueDTO mapVenueToDTO(Venue venue, UserDTO ownerDto) {
        String imageUrl = null;
        if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
            imageUrl = fileStorageService.getFileUrl(venue.getImagePath(), venuesBucketName);
        }
        return new VenueDTO(
                venue.getId(),
                venue.getName(),
                venue.getLocation(),
                ownerDto,
                imageUrl, // Use the generated URL
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }
}
