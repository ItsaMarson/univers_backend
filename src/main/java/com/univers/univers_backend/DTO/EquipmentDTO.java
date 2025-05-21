/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;
import java.time.Instant;
import java.util.Set;
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
        Set<EquipmentCategoryDTO> categories,
        Instant createdAt,
        Instant updatedAt,
        String serialNo) {}
