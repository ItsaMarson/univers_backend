package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

import com.univers.univers_backend.Enum.Status;

public record EquipmentDTO (
        Long id,
        String name,
        Boolean availability,
        String brand,
        Integer quantity,
        // Long equipmentOwner,
        UserDTO equipmentOwner, 
        String imagePath,
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
