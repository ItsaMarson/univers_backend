/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Mapper.UserMapper;
import com.univers.univers_backend.Repository.UserRepository;
import com.univers.univers_backend.Repository.VenueRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VenueService {

    private static final Logger logger = LoggerFactory.getLogger(VenueService.class);

    private final UserMapper userMapper;

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService; // Inject FileStorageService

    @Value("${minio.bucket.venues}") // Inject MinIO bucket name
    private String venuesBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    // Update constructor
    public VenueService(
            VenueRepository venueRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            UserMapper userMapper) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.userMapper = userMapper; // Add to constructor
    }

    @Transactional
    public VenueDTO addVenue(VenueDTO venueDTO, MultipartFile imageFile) {
        Optional<Venue> existingVenue = venueRepository.findByNameIgnoreCase(venueDTO.name());

        if (existingVenue.isPresent()) {
            throw new IllegalArgumentException("Venue already exists.");
        }
        Venue newVenue = new Venue();
        newVenue.setName(venueDTO.name());
        newVenue.setLocation(venueDTO.location());

        if (venueDTO.venueOwner() != null && venueDTO.venueOwner().publicId() != null) {
            User venueOwner =
                    userRepository
                            .findByPublicId(venueDTO.venueOwner().publicId())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "User (Venue Owner) not found with Public ID: "
                                                            + venueDTO.venueOwner().publicId()));
            newVenue.setVenueOwner(venueOwner);
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
        UserDTO finalOwnerDto = null;
        if (savedVenue.getVenueOwner() != null) {
            finalOwnerDto = userMapper.toDto(savedVenue.getVenueOwner());
        }

        return mapVenueToDTO(savedVenue, finalOwnerDto);
    }

    public List<VenueDTO> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();
        return venues.stream()
                .map(venue -> mapVenueToDTO(venue, userMapper.toDto(venue.getVenueOwner())))
                // update
                .collect(Collectors.toList());
    }

    public VenueDTO getVenueByPublicId(UUID venueId) {
        Venue venue =
                venueRepository
                        .findByPublicId(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with Public ID: " + venueId));
        return mapVenueToDTO(
                venue, userMapper.toDto(venue.getVenueOwner())); // mapVenueToDTO needs update
    }

    @Transactional
    public VenueDTO updateVenue(UUID venueId, VenueDTO venueDTO, MultipartFile imageFile) {
        Venue venue =
                venueRepository
                        .findByPublicId(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with Public ID: " + venueId));

        // Check for name conflict only if name is changing
        if (venueDTO.name() != null
                && !venueDTO.name().isBlank()
                && !venueDTO.name().equalsIgnoreCase(venue.getName())) {
            Optional<Venue> existingVenueWithName =
                    venueRepository.findByNameIgnoreCase(venueDTO.name());
            if (existingVenueWithName.isPresent()
                    && !existingVenueWithName.get().getId().equals(venue.getId())) {
                throw new IllegalArgumentException(
                        "Another venue with the name '" + venueDTO.name() + "' already exists.");
            }
            venue.setName(venueDTO.name());
        }

        if (venueDTO.location() != null && !venueDTO.location().isBlank()) {
            venue.setLocation(venueDTO.location());
        }

        // UserDTO ownerDto = null; // Not strictly needed here, will be mapped at the end.
        // Handle owner update
        if (venueDTO.venueOwner() != null
                && venueDTO.venueOwner().publicId() != null) { // Check publicId from DTO
            UUID newOwnerPublicId = venueDTO.venueOwner().publicId();
            if (venue.getVenueOwner() == null
                    || !venue.getVenueOwner().getPublicId().equals(newOwnerPublicId)) {
                User newVenueOwner =
                        userRepository
                                .findByPublicId(newOwnerPublicId) // Use findByPublicId
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "User (Venue Owner) not found with Public"
                                                                + " ID: "
                                                                + newOwnerPublicId));
                venue.setVenueOwner(newVenueOwner);
            }
            // If owner publicId matches, no change needed for owner entity.
        } else if (venueDTO.venueOwner() == null && venue.getVenueOwner() != null) {
            // Owner is being removed (explicitly set to null in DTO)
            venue.setVenueOwner(null);
        }
        // If venueDTO.venueOwner() is present but publicId is null, or if venueDTO.venueOwner() is
        // not present,
        // it implies no change to the existing venue owner unless explicitly set to null as above.

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

        UserDTO finalOwnerDto = null;
        if (updatedVenue.getVenueOwner() != null) {
            finalOwnerDto = userMapper.toDto(updatedVenue.getVenueOwner());
        }

        return mapVenueToDTO(updatedVenue, finalOwnerDto);
    }

    @Transactional
    public void deleteVenue(UUID venueId) {
        Venue venue =
                venueRepository
                        .findByPublicId(venueId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Venue not found with Public ID: " + venueId));

        // Delete image from MinIO if it exists
        if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
            fileStorageService.deleteFile(venue.getImagePath(), venuesBucketName);
        }

        // Now delete the venue record
        venueRepository.delete(venue);
    }

    @Transactional
    public void bulkDeleteVenues(List<UUID> venueIds) {
        List<Venue> venues = venueRepository.findAllByPublicIdIn(venueIds);

        // Check if all venues were found
        if (venues.size() != venueIds.size()) {
            Set<UUID> foundIds =
                    venues.stream().map(Venue::getPublicId).collect(Collectors.toSet());
            Set<UUID> notFoundIds =
                    venueIds.stream()
                            .filter(id -> !foundIds.contains(id))
                            .collect(Collectors.toSet());
            throw new NoSuchElementException("Some venues were not found: " + notFoundIds);
        }

        // Delete images from MinIO for all venues
        for (Venue venue : venues) {
            if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
                try {
                    fileStorageService.deleteFile(venue.getImagePath(), venuesBucketName);
                } catch (Exception e) {
                    // Log error but continue with deletion
                    logger.error(
                            "Error deleting image for venue {}: {}",
                            venue.getPublicId(),
                            e.getMessage());
                }
            }
        }

        // Delete all venues
        venueRepository.deleteAll(venues);
    }

    // Remove saveImage and deleteImage methods
    /*
     * private String saveImage(MultipartFile imageFile) { ... }
     * private void deleteImage(String imagePathString) { ... }
     */

    // Update mapVenueToDTO to generate URL from object name
    private VenueDTO mapVenueToDTO(Venue venue, UserDTO ownerDto) {
        String imageUrl = null;
        if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
            try {
                imageUrl = fileStorageService.getFileUrl(venue.getImagePath(), venuesBucketName);
            } catch (Exception e) {
                logger.error(
                        "Error generating image URL for venue {}: {}",
                        venue.getPublicId(),
                        e.getMessage());
            }
        }
        return new VenueDTO(
                venue.getPublicId(),
                venue.getName(),
                venue.getLocation(),
                ownerDto,
                imageUrl,
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }
}
