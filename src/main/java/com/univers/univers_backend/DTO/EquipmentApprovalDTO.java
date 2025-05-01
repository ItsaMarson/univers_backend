/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record EquipmentApprovalDTO(
        Long id,
        Long equipmentReservationId,
        Long userId,
        String signedBy,
        String userRole,
        String remarks,
        String status,
        LocalDateTime dateSigned) {}
