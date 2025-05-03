/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
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

    @Value("${minio.bucket.equipments}")
    private String equipmentsBucketName;

    @Value("${minio.bucket.users}")
    private String usersBucketName;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public EquipmentDTO addEquipment(Long userId, EquipmentDTO request, MultipartFile imageFile) {
        User requester =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found with ID: " + userId));

        if (!requester.getRoles().equals(Role.EQUIPMENT_OWNER)
                && !requester.getRoles().equals(Role.SUPER_ADMIN)) {
            throw new IllegalArgumentException("User is not authorized to add equipment.");
        }

        User owner;
        Long ownerIdFromRequest =
                (request.equipmentOwner() != null) ? request.equipmentOwner().id() : null;

        if (requester.getRoles().equals(Role.SUPER_ADMIN)) {
            if (ownerIdFromRequest == null) {
                throw new IllegalArgumentException(
                        "SUPER_ADMIN must specify the equipment owner ID in the request.");
            }
            owner =
                    userRepository
                            .findById(ownerIdFromRequest)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Specified Equipment Owner not found with ID: "
                                                            + ownerIdFromRequest));
            if (!owner.getRoles().equals(Role.EQUIPMENT_OWNER)) {
                throw new IllegalArgumentException(
                        "Specified user (ID: " + owner.getId() + ") is not an Equipment Owner.");
            }
        } else {
            owner = requester;
        }

        Equipment newEquipment = new Equipment();
        newEquipment.setName(request.name());
        newEquipment.setBrand(request.brand());
        newEquipment.setAvailability(request.availability());
        newEquipment.setQuantity(request.quantity());
        newEquipment.setStatus(request.status() != null ? request.status() : Status.NEW);
        newEquipment.setEquipmentOwner(owner);

        if (imageFile != null && !imageFile.isEmpty()) {
            String objectName =
                    fileStorageService.uploadFile(
                            imageFile, equipmentsBucketName, "equipment-images/");
            newEquipment.setImagePath(objectName);
        }

        Equipment savedEquipment = equipmentRepository.save(newEquipment);

        return mapToDTO(savedEquipment);
    }

    public List<EquipmentDTO> getAllEquipmentsByOwner(Long userId) {
        User owner =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User not found with ID: " + userId));

        List<Equipment> equipmentList = equipmentRepository.findAllByEquipmentOwner(owner);

        return equipmentList.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<EquipmentDTO> getAllEquipments() {
        List<Equipment> equipmentList = equipmentRepository.findAll();
        return equipmentList.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public EquipmentDTO getEquipmentById(Long equipmentId) {
        Equipment equipment =
                equipmentRepository
                        .findById(equipmentId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: " + equipmentId));
        return mapToDTO(equipment);
    }

    @Transactional
    public EquipmentDTO updateEquipment(
            Long equipmentId, Long userId, EquipmentDTO request, MultipartFile imageFile) {
        Equipment equipment =
                equipmentRepository
                        .findById(equipmentId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: " + equipmentId));

        User requester =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User (requester) not found with ID: " + userId));

        if (!equipment.getEquipmentOwner().getId().equals(userId)
                && !requester.getRoles().equals(Role.SUPER_ADMIN)) {
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
        if (request.quantity() != null) {
            equipment.setQuantity(request.quantity());
        }
        if (request.status() != null) {
            equipment.setStatus(request.status());
        }

        Long ownerIdFromRequest =
                (request.equipmentOwner() != null) ? request.equipmentOwner().id() : null;
        if (requester.getRoles().equals(Role.SUPER_ADMIN)
                && ownerIdFromRequest != null
                && !ownerIdFromRequest.equals(equipment.getEquipmentOwner().getId())) {
            User newOwner =
                    userRepository
                            .findById(ownerIdFromRequest)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Specified new Equipment Owner not found with"
                                                            + " ID: "
                                                            + ownerIdFromRequest));
            if (!newOwner.getRoles().equals(Role.EQUIPMENT_OWNER)) {
                throw new IllegalArgumentException(
                        "Specified new owner (ID: "
                                + newOwner.getId()
                                + ") is not an Equipment Owner.");
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
        return mapToDTO(updatedEquipment);
    }

    @Transactional
    public void deleteEquipment(Long equipmentId, Long userId) {
        Equipment equipment =
                equipmentRepository
                        .findById(equipmentId)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "Equipment not found with ID: " + equipmentId));

        User requester =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "User (requester) not found with ID: " + userId));

        if (!equipment.getEquipmentOwner().getId().equals(userId)
                && !requester.getRoles().equals(Role.SUPER_ADMIN)) {
            throw new IllegalArgumentException("User is not authorized to delete this equipment.");
        }

        if (equipment.getImagePath() != null && !equipment.getImagePath().isBlank()) {
            fileStorageService.deleteFile(equipment.getImagePath(), equipmentsBucketName);
        }

        equipmentRepository.deleteById(equipmentId);
    }

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

    // Update mapToDTO to generate URL from object name
    private EquipmentDTO mapToDTO(Equipment equipment) {
        UserDTO ownerDto = mapUserToDTO(equipment.getEquipmentOwner());
        String imageUrl = null;
        if (equipment.getImagePath() != null && !equipment.getImagePath().isBlank()) {
            imageUrl =
                    fileStorageService.getFileUrl(equipment.getImagePath(), equipmentsBucketName);
        }
        return new EquipmentDTO(
                equipment.getId(),
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
}
