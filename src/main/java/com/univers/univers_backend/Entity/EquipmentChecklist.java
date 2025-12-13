/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Task;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "equipment_checklist",
        indexes = {
            @Index(
                    name = "idx_equipment_checklist_event_personnel_id",
                    columnList = "event_personnel_id"),
            @Index(name = "idx_equipment_checklist_task", columnList = "task")
        })
public class EquipmentChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    @UuidGenerator
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_personnel_id")
    private EventPersonnel eventPersonnel;

    @Enumerated(EnumType.STRING)
    private Task task;

    @ElementCollection private List<String> equipmentIds;

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public EventPersonnel getEventPersonnel() {
        return eventPersonnel;
    }

    public void setEventPersonnel(EventPersonnel eventPersonnel) {
        this.eventPersonnel = eventPersonnel;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(List<String> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }
}
