/* (C)2025 */
package com.univers.univers_backend.DTO.dashboard;

import java.util.UUID;

public record TopDepartmentDTO(
        UUID departmentPublicId,
        String departmentName,
        Long totalEventCount,
        Long approvedCount,
        Long pendingCount,
        Long canceledCount,
        Long rejectedCount,
        Long ongoingCount,
        Long completedCount,
        Double reservationRate) {}
