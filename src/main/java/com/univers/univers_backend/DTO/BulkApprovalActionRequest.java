/* (C)2025 */
package com.univers.univers_backend.DTO;

import com.univers.univers_backend.Enum.Status;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BulkApprovalActionRequest(
        @NotEmpty List<UUID> eventPublicIds, @NotNull Status status, String remarks) {}
