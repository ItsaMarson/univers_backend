package com.univers.univers_backend.Service;


import com.univers.univers_backend.DTO.EquipmentDTO;
import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Role;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EquipmentRepository;
import com.univers.univers_backend.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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


    public EquipmentDTO addEquipment(Long userId, EquipmentDTO request, MultipartFile imagePath){
        Optional<User> userOptional = userRepository.findById(userId);

        if(userOptional.isEmpty()){
            throw new IllegalArgumentException("User not found");
        }
        User owner = userOptional.get();
        if(!owner.getRoles().equals(Role.EQUIPMENT_OWNER)){
            throw new IllegalArgumentException("You are not authorized to perform this action");
        }


        Equipment newEquipment = new Equipment();
        newEquipment.setName(request.name());
        newEquipment.setBrand(request.brand());
        newEquipment.setAvailability(request.availability());
        newEquipment.setQuantity(request.quantity());
        newEquipment.setStatus(request.status());
        newEquipment.setEquipmentOwner(owner);

        if(imagePath != null && !imagePath.isEmpty()){
            try{
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                if(imagePath.getOriginalFilename() != null){
                    Path filePath = uploadPath.resolve(imagePath.getOriginalFilename());
                    imagePath.transferTo(filePath.toFile());

                    newEquipment.setImagePath(filePath.toString());
                }

            }catch (IOException e){
                throw new RuntimeException("Failed to upload file. Please try again");
            }
        }

        Equipment savedEquipment = equipmentRepository.save(newEquipment);

        return new EquipmentDTO(
                savedEquipment.getId(),
                savedEquipment.getName(),
                savedEquipment.getAvailability(),
                savedEquipment.getBrand(),
                savedEquipment.getQuantity(),
                savedEquipment.getEquipmentOwner().getId(),
                savedEquipment.getImagePath(),
                savedEquipment.getStatus(),
                savedEquipment.getCreatedAt(),
                savedEquipment.getUpdatedAt()
        );
    }

    public List<EquipmentDTO> getAllEquipmentsByOwner(Long userId){
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isEmpty()){
            throw new IllegalArgumentException("User not found");
        }
        User owner = userOptional.get();
        List<Equipment> equipmentList = equipmentRepository.findAllByEquipmentOwner(owner);

        return equipmentList.stream()
                .map(equipment -> new EquipmentDTO(
                        equipment.getId(),
                        equipment.getName(),
                        equipment.getAvailability(),
                        equipment.getBrand(),
                        equipment.getQuantity(),
                        equipment.getEquipmentOwner().getId(),
                        equipment.getImagePath(),
                        equipment.getStatus(),
                        equipment.getCreatedAt(),
                        equipment.getUpdatedAt()
                )).collect(Collectors.toList());
    }

}
