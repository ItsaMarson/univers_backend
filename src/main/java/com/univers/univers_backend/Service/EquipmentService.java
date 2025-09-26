/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.EquipmentInputDTO;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.EquipmentCategory;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.EquipmentMapper;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentCategoryService equipmentCategoryService;

    @Value("${minio.bucket.equipments}")
    private String equipmentsBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            EquipmentCategoryService equipmentCategoryService,
            EquipmentMapper equipmentMapper) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.equipmentCategoryService = equipmentCategoryService;
        this.equipmentMapper = equipmentMapper;
    }

    private Set<EquipmentCategory> resolveCategoriesByIds(Set<String> categoryPublicIds) {
        if (categoryPublicIds == null || categoryPublicIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<EquipmentCategory> resolvedCategories = new HashSet<>();
        for (String catId : categoryPublicIds) {
            EquipmentCategory category =
                    equipmentCategoryService
                            .findByPublicId(UUID.fromString(catId))
                            .orElseThrow(
                                    () ->
                                            new NoSuchElementException(
                                                    "EquipmentCategory not found with public ID: "
                                                            + catId));
            resolvedCategories.add(category);
        }
        return resolvedCategories;
    }

    @Transactional
    public EquipmentDTO addEquipment(
            String userId, EquipmentInputDTO request, MultipartFile imageFile) {
        User requester =
                userRepository
                        .findByPublicId(UUID.fromString(userId))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found with Public ID: " + userId));

        Set<Role> authorizedRoles = Set.of(Role.EQUIPMENT_OWNER, Role.SUPER_ADMIN);
        if (requester.getRoles().stream().noneMatch(authorizedRoles::contains)) {
            throw new IllegalArgumentException("User is not authorized to add equipment.");
        }

        User owner;
        UUID ownerPublicIdFromRequest =
                (request.equipmentOwner() != null && request.equipmentOwner().publicId() != null)
                        ? request.equipmentOwner().publicId()
                        : null;

        if (requester.getRoles().contains(Role.SUPER_ADMIN)) {
            if (ownerPublicIdFromRequest == null) {
                throw new IllegalArgumentException(
                        "SUPER_ADMIN must specify the equipment owner's publicId in the request.");
            }
            owner =
                    userRepository
                            .findByPublicId(ownerPublicIdFromRequest)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Specified Equipment Owner not found with"
                                                            + " Public ID: "
                                                            + ownerPublicIdFromRequest));
            if (owner.getRoles().stream().noneMatch(authorizedRoles::contains)) {
                throw new IllegalArgumentException(
                        "Specified user (Public ID: "
                                + owner.getPublicId()
                                + ") is not an authorized Equipment Manager (EQUIPMENT_OWNER, MSDO,"
                                + " OPC).");
            }
        } else {
            owner = requester;
        }

        Equipment newEquipment = new Equipment();
        newEquipment.setName(request.name());
        newEquipment.setBrand(request.brand());
        newEquipment.setAvailability(request.availability());

        // Handle quantity fields - prioritize new fields over deprecated one
        Integer totalQty =
                request.totalQuantity() != null ? request.totalQuantity() : request.quantity();
        Integer availableQty =
                request.availableQuantity() != null ? request.availableQuantity() : totalQty;

        if (totalQty != null && totalQty < 0) {
            throw new IllegalArgumentException("Equipment total quantity cannot be negative.");
        }
        if (availableQty != null && availableQty < 0) {
            throw new IllegalArgumentException("Equipment available quantity cannot be negative.");
        }
        if (totalQty != null && availableQty != null && availableQty > totalQty) {
            throw new IllegalArgumentException("Available quantity cannot exceed total quantity.");
        }

        newEquipment.setTotalQuantity(totalQty);
        newEquipment.setAvailableQuantity(availableQty);
        newEquipment.setStatus(request.status() != null ? request.status() : Status.NEW);
        newEquipment.setEquipmentOwner(owner);
        String serial = request.serialNo();
        if (serial != null && serial.isBlank()) {
            serial = null;
        }

        equipmentRepository
                .findBySerialNo(serial)
                .ifPresent(
                        existingEquipment -> {
                            if (!existingEquipment.getPublicId().equals(requester.getPublicId())) {
                                throw new IllegalArgumentException(
                                        "Another equipment with serial number '"
                                                + request.serialNo()
                                                + "' already exists.");
                            }
                        });
        newEquipment.setSerialNo(serial);

        Set<EquipmentCategory> resolvedCategories = resolveCategoriesByIds(request.categoryIds());
        newEquipment.setCategories(resolvedCategories);

        if (imageFile != null && !imageFile.isEmpty()) {
            String objectName =
                    fileStorageService.uploadFile(
                            imageFile, equipmentsBucketName, "equipment-images/");
            newEquipment.setImagePath(objectName);
        }

        Equipment savedEquipment = equipmentRepository.save(newEquipment);

        return equipmentMapper.toDto(savedEquipment);
    }

    public List<EquipmentDTO> getAllEquipmentsByOwner(String userId) {
        User owner =
                userRepository
                        .findByPublicId(UUID.fromString(userId))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found with ID: " + userId));

        List<Equipment> equipmentList = equipmentRepository.findAllByEquipmentOwner(owner);

        return equipmentList.stream().map(equipmentMapper::toDto).collect(Collectors.toList());
    }

    public List<EquipmentDTO> getAllEquipments() {
        List<Equipment> equipmentList = equipmentRepository.findAll();
        return equipmentList.stream().map(equipmentMapper::toDto).collect(Collectors.toList());
    }

    public EquipmentDTO getEquipmentById(String equipmentId) {
        Equipment equipment =
                equipmentRepository
                        .findByPublicId(UUID.fromString(equipmentId))
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: " + equipmentId));
        return equipmentMapper.toDto(equipment);
    }

    @Transactional
    public EquipmentDTO updateEquipment(
            String equipmentId, String userId, EquipmentInputDTO request, MultipartFile imageFile) {
        Equipment equipment =
                equipmentRepository
                        .findByPublicId(UUID.fromString(equipmentId))
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: " + equipmentId));

        User requester =
                userRepository
                        .findByPublicId(UUID.fromString(userId))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User (requester) not found with ID: " + userId));

        Set<Role> equipmentManagerRoles = Set.of(Role.EQUIPMENT_OWNER);

        if (requester.getRoles().stream().noneMatch(role -> role == Role.SUPER_ADMIN)
                && !(requester.getRoles().stream().anyMatch(equipmentManagerRoles::contains)
                        && equipment.getEquipmentOwner() != null
                        && equipment
                                .getEquipmentOwner()
                                .getPublicId()
                                .equals(UUID.fromString(userId)))) {
            throw new IllegalArgumentException("User is not authorized to update this equipment.");
        }

        if (request.name() != null && !request.name().isBlank()) {
            equipment.setName(request.name());
        }
        if (request.brand() != null && !request.brand().isBlank()) {
            equipment.setBrand(request.brand());
        }
        if (request.availability() != null) {
            equipment.setAvailability(request.availability());
        }
        // Handle quantity updates - prioritize new fields over deprecated one
        Integer newTotalQty =
                request.totalQuantity() != null ? request.totalQuantity() : request.quantity();
        Integer newAvailableQty = request.availableQuantity();

        if (newTotalQty != null) {
            if (newTotalQty < 0) {
                throw new IllegalArgumentException("Equipment total quantity cannot be negative.");
            }

            Integer currentReserved =
                    equipment.getTotalQuantity() != null && equipment.getAvailableQuantity() != null
                            ? equipment.getTotalQuantity() - equipment.getAvailableQuantity()
                            : 0;

            if (newTotalQty < currentReserved) {
                throw new IllegalArgumentException(
                        String.format(
                                "Cannot reduce total quantity below currently reserved amount. "
                                        + "Requested: %d, Currently reserved: %d",
                                newTotalQty, currentReserved));
            }

            equipment.setTotalQuantity(newTotalQty);

            // If availableQuantity is not explicitly set, calculate it
            if (newAvailableQty == null) {
                equipment.setAvailableQuantity(newTotalQty - currentReserved);
            }
        }

        if (newAvailableQty != null) {
            if (newAvailableQty < 0) {
                throw new IllegalArgumentException(
                        "Equipment available quantity cannot be negative.");
            }

            Integer currentTotalQty = equipment.getTotalQuantity();
            if (currentTotalQty != null && newAvailableQty > currentTotalQty) {
                throw new IllegalArgumentException(
                        "Available quantity cannot exceed total quantity.");
            }

            equipment.setAvailableQuantity(newAvailableQty);
        }
        if (request.status() != null) {
            equipment.setStatus(request.status());
        }
        if (request.serialNo() != null && !request.serialNo().isBlank()) {
            if (!equipment.getSerialNo().equals(request.serialNo())) {
                equipmentRepository
                        .findBySerialNo(request.serialNo())
                        .ifPresent(
                                existingEquipment -> {
                                    if (!existingEquipment
                                            .getPublicId()
                                            .equals(equipment.getPublicId())) {
                                        throw new IllegalArgumentException(
                                                "Another equipment with serial number '"
                                                        + request.serialNo()
                                                        + "' already exists.");
                                    }
                                });
                equipment.setSerialNo(request.serialNo());
            }
        }

        if (request.categoryIds() != null) {
            Set<EquipmentCategory> resolvedCategories =
                    resolveCategoriesByIds(request.categoryIds());
            equipment.setCategories(resolvedCategories);
        }

        UUID newOwnerPublicIdFromRequest =
                (request.equipmentOwner() != null && request.equipmentOwner().publicId() != null)
                        ? request.equipmentOwner().publicId()
                        : null;

        if (requester.getRoles().contains(Role.SUPER_ADMIN)
                && newOwnerPublicIdFromRequest != null
                && (equipment.getEquipmentOwner() == null
                        || !newOwnerPublicIdFromRequest.equals(
                                equipment.getEquipmentOwner().getPublicId()))) {
            User newOwner =
                    userRepository
                            .findByPublicId(newOwnerPublicIdFromRequest)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Specified new Equipment Owner not found with"
                                                            + " Public ID: "
                                                            + newOwnerPublicIdFromRequest));

            if (newOwner.getRoles().stream().noneMatch(equipmentManagerRoles::contains)
                    && newOwner.getRoles().stream().noneMatch(role -> role == Role.SUPER_ADMIN)) {
                throw new IllegalArgumentException(
                        "Specified new owner (Public ID: "
                                + newOwner.getPublicId()
                                + ") is not an authorized Equipment Manager (EQUIPMENT_OWNER, MSDO,"
                                + " OPC).");
            }
            equipment.setEquipmentOwner(newOwner);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (equipment.getImagePath() != null && !equipment.getImagePath().isBlank()) {
                fileStorageService.deleteFile(equipment.getImagePath(), equipmentsBucketName);
            }
            String newObjectName =
                    fileStorageService.uploadFile(
                            imageFile, equipmentsBucketName, "equipment-images/");
            equipment.setImagePath(newObjectName);
        }

        Equipment updatedEquipment = equipmentRepository.save(equipment);
        return equipmentMapper.toDto(updatedEquipment);
    }

    @Transactional
    public Map<String, String> bulkDeleteEquipments(List<UUID> equipmentIds, String userId) {
        Map<String, String> results = new HashMap<>();
        User requester =
                userRepository
                        .findByPublicId(UUID.fromString(userId))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User (requester) not found with ID: " + userId));

        Set<Role> equipmentManagerRoles = Set.of(Role.EQUIPMENT_OWNER);
        boolean isSuperAdmin =
                requester.getRoles().stream().anyMatch(role -> role == Role.SUPER_ADMIN);

        for (UUID equipmentId : equipmentIds) {
            try {
                Equipment equipment =
                        equipmentRepository
                                .findByPublicId(equipmentId)
                                .orElseThrow(
                                        () ->
                                                new NoSuchElementException(
                                                        "Equipment not found with ID: "
                                                                + equipmentId));

                if (!isSuperAdmin
                        && !(requester.getRoles().stream().anyMatch(equipmentManagerRoles::contains)
                                && equipment.getEquipmentOwner() != null
                                && equipment
                                        .getEquipmentOwner()
                                        .getPublicId()
                                        .equals(UUID.fromString(userId)))) {
                    throw new SecurityException("User is not authorized to delete this equipment.");
                }

                // Before deleting equipment, disassociate categories to avoid constraint violations
                equipment.getCategories().clear();
                equipmentRepository.save(equipment);

                if (equipment.getImagePath() != null && !equipment.getImagePath().isBlank()) {
                    fileStorageService.deleteFile(equipment.getImagePath(), equipmentsBucketName);
                }

                equipmentRepository.delete(equipment);
                results.put(equipmentId.toString(), "Successfully deleted");
            } catch (Exception e) {
                results.put(equipmentId.toString(), "Error: " + e.getMessage());
            }
        }
        return results;
    }
}
