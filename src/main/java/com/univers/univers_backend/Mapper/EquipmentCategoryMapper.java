/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.EquipmentCategoryDTO;
import com.univers.univers_backend.Entity.EquipmentCategory;
import org.springframework.stereotype.Component;

@Component
public class EquipmentCategoryMapper {

    public EquipmentCategoryDTO toDto(EquipmentCategory category) {
        if (category == null) {
            return null;
        }
        return new EquipmentCategoryDTO(
                category.getPublicId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public EquipmentCategory toEntity(EquipmentCategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        EquipmentCategory category = new EquipmentCategory();
        // Note: publicId is typically not set from DTO for new entities,
        // it's generated. For updates, it would be used to fetch the entity.
        // We'll assume this toEntity is for creation or non-ID-sensitive updates.
        category.setName(dto.name());
        category.setDescription(dto.description());
        // createdAt and updatedAt are usually managed by @PrePersist/@PreUpdate
        return category;
    }
}
