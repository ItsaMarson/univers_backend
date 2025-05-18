/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record EquipmentApprovalDTO(
        UUID publicId,
        UUID equipmentReservationPublicId,
        UserDTO signedByUser,
        Set<String> userRole,
        String remarks,
        String status,
        Instant dateSigned) {}
