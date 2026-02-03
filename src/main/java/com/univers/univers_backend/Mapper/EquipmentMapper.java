/* (C)2025-2026 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.EquipmentCategoryDTO;
import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Service.FileStorageService;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EquipmentMapper {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentMapper.class);

    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final String equipmentBucketName;
    private final EquipmentCategoryMapper equipmentCategoryMapper;

    public EquipmentMapper(
            @Lazy UserMapper userMapper,
            FileStorageService fileStorageService,
            EquipmentCategoryMapper equipmentCategoryMapper,
            @Value("${storage.bucket.equipments}") String equipmentBucketName) {
        this.userMapper = userMapper;
        this.fileStorageService = fileStorageService;
        this.equipmentBucketName = equipmentBucketName;
        this.equipmentCategoryMapper = equipmentCategoryMapper;
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
                logger.error(
                        "Error generating image URL for equipment {}", equipment.getPublicId(), e);
            }
        }
        Set<EquipmentCategoryDTO> categoryDTOs = Collections.emptySet();
        if (equipment.getCategories() != null) {
            categoryDTOs =
                    equipment.getCategories().stream()
                            .map(equipmentCategoryMapper::toDto)
                            .collect(Collectors.toSet());
        }

        return new EquipmentDTO(
                equipment.getPublicId(),
                equipment.getName(),
                equipment.getAvailability(),
                equipment.getBrand(),
                equipment.getTotalQuantity(),
                equipment.getAvailableQuantity(),
                equipment.getQuantity(), // deprecated backward compatibility
                ownerDto,
                imageUrl,
                equipment.getStatus(),
                categoryDTOs,
                equipment.getCreatedAt(),
                equipment.getUpdatedAt(),
                equipment.getSerialNo());
    }

    // public Equipment toEntity(EquipmentDTO dto) { ... } // If needed later
}
