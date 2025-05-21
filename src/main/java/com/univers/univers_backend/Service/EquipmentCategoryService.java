/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentCategoryDTO;
import com.univers.univers_backend.Entity.EquipmentCategory;
import com.univers.univers_backend.Mapper.EquipmentCategoryMapper;
import com.univers.univers_backend.Repository.EquipmentCategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentCategoryService {

    private final EquipmentCategoryRepository categoryRepository;
    private final EquipmentCategoryMapper categoryMapper;

    public EquipmentCategoryService(
            EquipmentCategoryRepository categoryRepository,
            EquipmentCategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public EquipmentCategoryDTO createCategory(EquipmentCategoryDTO categoryDTO) {
        if (categoryDTO.name() == null || categoryDTO.name().isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }
        if (categoryRepository.findByName(categoryDTO.name()).isPresent()) {
            throw new IllegalArgumentException(
                    "Category with name '" + categoryDTO.name() + "' already exists.");
        }

        EquipmentCategory category = categoryMapper.toEntity(categoryDTO);
        // publicId is set via @PrePersist in the entity
        EquipmentCategory savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<EquipmentCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EquipmentCategoryDTO getCategoryByPublicId(String publicIdStr) {
        UUID publicId;
        try {
            publicId = UUID.fromString(publicIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid public ID format for category.");
        }
        return categoryRepository
                .findByPublicId(publicId)
                .map(categoryMapper::toDto)
                .orElseThrow(
                        () ->
                                new NoSuchElementException(
                                        "Category not found with public ID: " + publicIdStr));
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentCategory> findByPublicId(UUID publicId) {
        return categoryRepository.findByPublicId(publicId);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentCategory> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Transactional
    public EquipmentCategoryDTO updateCategory(
            String publicIdStr, EquipmentCategoryDTO categoryDTO) {
        UUID publicId;
        try {
            publicId = UUID.fromString(publicIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid public ID format for category update.");
        }

        EquipmentCategory existingCategory =
                categoryRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Category not found with public ID: "
                                                        + publicIdStr
                                                        + " for update."));

        if (categoryDTO.name() != null && !categoryDTO.name().isBlank()) {
            // Check if new name conflicts with another existing category
            Optional<EquipmentCategory> categoryWithNewName =
                    categoryRepository.findByName(categoryDTO.name());
            if (categoryWithNewName.isPresent()
                    && !categoryWithNewName.get().getPublicId().equals(publicId)) {
                throw new IllegalArgumentException(
                        "Another category with name '" + categoryDTO.name() + "' already exists.");
            }
            existingCategory.setName(categoryDTO.name());
        }
        if (categoryDTO.description() != null) {
            existingCategory.setDescription(categoryDTO.description());
        }
        // createdAt and publicId are not updatable
        // updatedAt is handled by @PreUpdate

        EquipmentCategory updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(String publicIdStr) {
        UUID publicId;
        try {
            publicId = UUID.fromString(publicIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid public ID format for category deletion.");
        }

        EquipmentCategory categoryToDelete =
                categoryRepository
                        .findByPublicId(publicId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Category not found with public ID: "
                                                        + publicIdStr
                                                        + " for deletion."));

        // Before deleting, ensure no equipment is associated with this category.
        // The @ManyToMany relationship in EquipmentCategory has `mappedBy`.
        // We need to check if `categoryToDelete.getEquipments()` is empty.
        // If not empty, it means there are equipments linked, and we should prevent deletion
        // or handle it (e.g., by disassociating them first, depending on requirements).
        // For now, let's prevent deletion if associated.
        if (!categoryToDelete.getEquipments().isEmpty()) {
            throw new IllegalStateException(
                    "Category '"
                            + categoryToDelete.getName()
                            + "' cannot be deleted as it is associated with existing equipment.");
        }

        categoryRepository.delete(categoryToDelete);
    }
}
