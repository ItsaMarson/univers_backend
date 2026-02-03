/* (C)2025-2026 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueDTO;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final String venuesBucketName;

    public VenueMapper(
            @Lazy UserMapper userMapper,
            FileStorageService fileStorageService,
            @Value("${storage.bucket.venues}") String venuesBucketName) {
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
        this.venuesBucketName = venuesBucketName;
    }

    public VenueDTO toDto(Venue venue) {
        if (venue == null) {
            return null;
        }

        UserDTO venueOwnerDto = userMapper.toDto(venue.getVenueOwner());
        String imageUrl = null;
        if (venue.getImagePath() != null && !venue.getImagePath().isBlank()) {
            try {
                imageUrl = fileStorageService.getFileUrl(venue.getImagePath(), venuesBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for venue "
                                + venue.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        return new VenueDTO(
                venue.getPublicId(),
                venue.getName(),
                venue.getLocation(),
                venueOwnerDto,
                imageUrl,
                venue.getCreatedAt(),
                venue.getUpdatedAt());
    }

    // public Venue toEntity(VenueDTO dto) { ... } // If needed later
}
