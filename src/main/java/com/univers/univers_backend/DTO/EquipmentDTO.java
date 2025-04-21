package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;

import java.time.LocalDateTime;

public record EquipmentDTO (
        Long id,
        String name,
        Boolean availability,
        String brand,
        Integer quantity,
        Long equipmentOwner,
        String imagePath,
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){
}
