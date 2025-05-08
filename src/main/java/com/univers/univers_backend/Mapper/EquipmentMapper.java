/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EquipmentMapper {

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final String equipmentBucketName;

    public EquipmentMapper(
            @Lazy UserMapper userMapper,
            FileStorageService fileStorageService,
            @Value("${minio.bucket.equipments}") String equipmentBucketName) {
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
        this.equipmentBucketName = equipmentBucketName;
    }

    public EquipmentDTO toDto(Equipment equipment) {
        if (equipment == null) {
            return null;
        }

        UserDTO ownerDto = userMapper.toDto(equipment.getEquipmentOwner());
        String imageUrl = null;
        if (equipment.getImagePath() != null && !equipment.getImagePath().isBlank()) {
            try {
                imageUrl =
                        fileStorageService.getFileUrl(
                                equipment.getImagePath(), equipmentBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for equipment "
                                + equipment.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        return new EquipmentDTO(
                equipment.getPublicId(),
                equipment.getName(),
                equipment.getAvailability(),
                equipment.getBrand(),
                equipment.getQuantity(),
                ownerDto,
                imageUrl,
                equipment.getStatus(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt());
    }

    // public Equipment toEntity(EquipmentDTO dto) { ... } // If needed later
}
