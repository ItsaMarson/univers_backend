/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;
import java.util.Set;

// DTO for creating/updating equipment, accepting categoryIds as strings
public record EquipmentInputDTO(
        // publicId is not part of input body, usually in path for update
        String name,
        Boolean availability,
        String brand,
        @Deprecated Integer quantity, // Deprecated - use totalQuantity instead
        Integer totalQuantity,
        Integer availableQuantity,
        UserDTO equipmentOwner, // Frontend sends ownerId, which maps to UserDTO with publicId
        // imagePath is handled by MultipartFile, not in this DTO
        Status status,
        Set<String> categoryIds, // Accepts category public IDs as strings
        // createdAt and updatedAt are set by the backend
        String serialNo) {}
