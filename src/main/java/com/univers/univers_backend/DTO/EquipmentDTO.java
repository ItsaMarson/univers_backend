/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;
import java.time.LocalDateTime;
import java.util.UUID;

public record EquipmentDTO(
        UUID publicId,
        String name,
        Boolean availability,
        String brand,
        Integer quantity,
        UserDTO equipmentOwner,
        String imagePath,
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
