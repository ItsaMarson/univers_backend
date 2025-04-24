package com.univers.univers_backend.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.DTO.UserDTO;
// Added import for UserDTO
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Value("${upload.equipment.dir}")
    private String uploadDir;

    public EquipmentService(EquipmentRepository equipmentRepository, UserRepository userRepository){
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
    }


    public EquipmentDTO addEquipment(Long userId, EquipmentDTO request, MultipartFile imageFile){ 
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Allow SUPER_ADMIN or EQUIPMENT_OWNER to add equipment
        if(!requester.getRoles().equals(Role.EQUIPMENT_OWNER) && !requester.getRoles().equals(Role.SUPER_ADMIN)){
            throw new IllegalArgumentException("User is not authorized to add equipment.");
        }

       
        // SUPER_ADMIN can add *for* an EQUIPMENT_OWNER.
        // We need the intended owner's ID. Let's assume it's in the request DTO for now.
        // Note: The request DTO still expects equipmentOwnerId (or null if owner is requester)
        // We handle this logic here, but the response DTO will contain the full UserDTO.

        User owner;
        Long ownerIdFromRequest = (request.equipmentOwner() != null) ? request.equipmentOwner().id() : null;

        if (requester.getRoles().equals(Role.SUPER_ADMIN)) {
            if (ownerIdFromRequest == null) {
                 throw new IllegalArgumentException("SUPER_ADMIN must specify the equipment owner ID in the request.");
            }
            owner = userRepository.findById(ownerIdFromRequest)
                 .orElseThrow(() -> new IllegalArgumentException("Specified Equipment Owner not found with ID: " + ownerIdFromRequest));
            if (!owner.getRoles().equals(Role.EQUIPMENT_OWNER)) {
                 throw new IllegalArgumentException("Specified user (ID: " + owner.getId() + ") is not an Equipment Owner.");
            }
        } else {
            owner = requester; 
        }


        Equipment newEquipment = new Equipment();
        newEquipment.setName(request.name());
        newEquipment.setBrand(request.brand());
        newEquipment.setAvailability(request.availability());
        newEquipment.setQuantity(request.quantity());
        newEquipment.setStatus(request.status() != null ? request.status() : Status.NEW); // Default to NEW if null
        newEquipment.setEquipmentOwner(owner); 

        if(imageFile != null && !imageFile.isEmpty()){
            String imagePath = saveImage(imageFile); 
            newEquipment.setImagePath(imagePath);
        }

        Equipment savedEquipment = equipmentRepository.save(newEquipment);

        return mapToDTO(savedEquipment);
    }

    public List<EquipmentDTO> getAllEquipmentsByOwner(Long userId){
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<Equipment> equipmentList = equipmentRepository.findAllByEquipmentOwner(owner);

        return equipmentList.stream()
                .map(this::mapToDTO) 
                .collect(Collectors.toList());
    }

    public List<EquipmentDTO> getAllEquipments() {
        List<Equipment> equipmentList = equipmentRepository.findAll();
        return equipmentList.stream()
                .map(this::mapToDTO) 
                .collect(Collectors.toList());
    }

    public EquipmentDTO getEquipmentById(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found with ID: " + equipmentId));
        return mapToDTO(equipment); 
    }

    @Transactional
    public EquipmentDTO updateEquipment(Long equipmentId, Long userId, EquipmentDTO request, MultipartFile imageFile) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found with ID: " + equipmentId));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User (requester) not found with ID: " + userId));

        if (!equipment.getEquipmentOwner().getId().equals(userId) && !requester.getRoles().equals(Role.SUPER_ADMIN)) {
            throw new IllegalArgumentException("User is not authorized to update this equipment.");
        }

        if (request.name() != null) {
            equipment.setName(request.name());
        }
        if (request.brand() != null) {
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

        Long ownerIdFromRequest = (request.equipmentOwner() != null) ? request.equipmentOwner().id() : null; 

        if (requester.getRoles().equals(Role.SUPER_ADMIN) && ownerIdFromRequest != null && !ownerIdFromRequest.equals(equipment.getEquipmentOwner().getId())) {
             User newOwner = userRepository.findById(ownerIdFromRequest)
                 .orElseThrow(() -> new IllegalArgumentException("Specified new Equipment Owner not found with ID: " + ownerIdFromRequest));
             if (!newOwner.getRoles().equals(Role.EQUIPMENT_OWNER)) {
                 throw new IllegalArgumentException("Specified new owner (ID: " + newOwner.getId() + ") is not an Equipment Owner.");
             }
             equipment.setEquipmentOwner(newOwner);
        }


        if (imageFile != null && !imageFile.isEmpty()) {
            deleteImage(equipment.getImagePath()); // Delete the old image if it exists
            String newImagePath = saveImage(imageFile); 
            equipment.setImagePath(newImagePath);
        }

        Equipment updatedEquipment = equipmentRepository.save(equipment);
        return mapToDTO(updatedEquipment); 
    }

    @Transactional
    public void deleteEquipment(Long equipmentId, Long userId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new NoSuchElementException("Equipment not found with ID: " + equipmentId));

         User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User (requester) not found with ID: " + userId));

        if (!equipment.getEquipmentOwner().getId().equals(userId) && !requester.getRoles().equals(Role.SUPER_ADMIN)) {
            throw new IllegalArgumentException("User is not authorized to delete this equipment.");
        }

        deleteImage(equipment.getImagePath());


        equipmentRepository.deleteById(equipmentId);
    }

    private String saveImage(MultipartFile imageFile) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename == null) {
                 throw new RuntimeException("Image file name is null.");
            }
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            String uniqueFilename = System.currentTimeMillis() + "_" + sanitizedFilename;

            Path filePath = uploadPath.resolve(uniqueFilename);
            imageFile.transferTo(filePath.toFile());
      
            return filePath.toString(); 
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image file. Please try again.", e);
        }
    }

    private void deleteImage(String imagePathString) {
        if (imagePathString != null && !imagePathString.isEmpty()) {
            try {
                 Path imagePath = Paths.get(imagePathString);
                 Files.deleteIfExists(imagePath);
             } catch (IOException e) {
                 System.err.println("Failed to delete image file: " + imagePathString + ". Error: " + e.getMessage());
             }
        }
    }

    private UserDTO mapUserToDTO(User user) {
        if (user == null) return null;
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
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }


    private EquipmentDTO mapToDTO(Equipment equipment) {
        UserDTO ownerDto = mapUserToDTO(equipment.getEquipmentOwner()); 
        return new EquipmentDTO(
                equipment.getId(),
                equipment.getName(),
                equipment.getAvailability(),
                equipment.getBrand(),
                equipment.getQuantity(),
                ownerDto, 
                equipment.getImagePath(),
                equipment.getStatus(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt()
        );
    }
}