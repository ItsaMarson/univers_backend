/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.UUID;

public record EquipmentCategoryDTO(
        UUID publicId, String name, String description, Instant createdAt, Instant updatedAt) {}
