package com.univers.univers_backend.DTO;

import java.time.LocalDateTime;

public record EventApprovalDTO(
        Long id,
        Long eventId,
        String department,
        String signedBy,
        String remarks,
        String status,
        LocalDateTime dateSigned
) {
}
