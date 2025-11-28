/* (C)2025 */
package com.univers.univers_backend.DTO;

import java.util.List;
import java.util.UUID;

public record EquipmentChecklistStatusDTO(
        String equipmentId, boolean checked, List<UUID> checkedByPersonnelIds) {}
