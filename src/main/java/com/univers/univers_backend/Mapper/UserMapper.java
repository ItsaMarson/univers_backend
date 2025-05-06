/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final FileStorageService fileStorageService;
    private final String usersBucketName;

    public UserMapper(
            FileStorageService fileStorageService,
            @Value("${minio.bucket.users}") String usersBucketName) {
        this.fileStorageService = fileStorageService;
        this.usersBucketName = usersBucketName;
    }

    public UserDTO toDto(User user) {
        if (user == null) {
            return null;
        }
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
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles() != null ? user.getRoles().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
