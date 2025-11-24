package com.univers.univers_backend.DTO;

import java.util.List;
import java.util.UUID;

public record EquipmentChecklistRequest(
        UUID eventPersonnelId,
        List<String> equipmentIds
) {
}
