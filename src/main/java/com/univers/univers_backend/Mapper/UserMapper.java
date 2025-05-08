/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.DepartmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final FileStorageService fileStorageService;
    private final String usersBucketName;
    private final DepartmentMapper departmentMapper;

    public UserMapper(
            FileStorageService fileStorageService,
            @Value("${minio.bucket.users}") String usersBucketName,
            @Lazy DepartmentMapper departmentMapper) {
        this.fileStorageService = fileStorageService;
        this.usersBucketName = usersBucketName;
        this.departmentMapper = departmentMapper;
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
                                + user.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        DepartmentDTO departmentDto = null;
        if (user.getDepartment() != null) {
            departmentDto = departmentMapper.toDto(user.getDepartment());
        }

        return new UserDTO(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles() != null ? user.getRoles().name() : null,
                departmentDto,
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public UserDTO toDtoWithoutDepartment(User user) {
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
                                + user.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        // Intentionally skip mapping the department to break cycles
        DepartmentDTO departmentDto = null; 

        return new UserDTO(
                user.getPublicId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getId_number(),
                user.getPhone_number(),
                user.getTelephoneNumber(),
                user.getRoles() != null ? user.getRoles().name() : null,
                departmentDto, // This will be null
                user.getEmailVerified(),
                user.isActive(),
                profileImageUrl,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
