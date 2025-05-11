/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EquipmentReservationDTO(
        UUID publicId,
        EventDTO event,
        UserDTO requestingUser,
        DepartmentDTO department,
        EquipmentDTO equipment,
        Integer quantity,
        Instant startTime,
        Instant endTime,
        String status,
        List<EquipmentApprovalDTO> approvals,
        Instant createdAt,
        Instant updatedAt) {}
