/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EquipmentReservationDTO(
        UUID publicId,
        EventDTO event,
        UserDTO requestingUser,
        DepartmentDTO department,
        EquipmentDTO equipment,
        Integer quantity,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        List<EquipmentApprovalDTO> approvals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
