/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.EquipmentChecklist;
import com.univers.univers_backend.Enum.Task;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentChecklistRepository extends JpaRepository<EquipmentChecklist, Long> {

    List<EquipmentChecklist> findByEventPersonnel_Event_PublicIdAndTask(
            UUID eventPublicId, Task task);
}
