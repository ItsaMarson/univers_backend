/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;
import java.util.List;

public record EquipmentReservationDTO(
        Long id,
        Long eventId,
        String eventName,
        UserDTO requestingUser,
        Long departmentId,
        String departmentName,
        Long equipmentId,
        String equipmentName,
        Integer quantity,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String reservationLetterUrl,
        List<EquipmentApprovalDTO> approvals,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
