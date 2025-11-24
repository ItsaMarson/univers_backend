/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Enum.Task;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "event_personnel")
public class EventPersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    @UuidGenerator
    private UUID publicId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_personnel_id", nullable = false)
    private User assignedPersonnel;

    @OneToMany(mappedBy = "eventPersonnel", cascade = CascadeType.ALL)
    private List<EquipmentChecklist> checklists = new ArrayList<>();
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ElementCollection
    private List<String> assignedEquipmentIds;


    public EventPersonnel(
            Long id,
            UUID publicId,
            User assignedPersonnel,
            List<EquipmentChecklist> checklists,
            String phoneNumber,
            Status status,
            Task task,
            List<String> assignedEquipmentIds) {
        this.id = id;
        this.publicId = publicId;
        this.assignedPersonnel = assignedPersonnel;
        this.checklists = checklists;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.task = task;
        this.assignedPersonnel = assignedPersonnel;
    }

    public EventPersonnel() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public User getAssignedPersonnel() {
        return assignedPersonnel;
    }

    public void setAssignedPersonnel(User assignedPersonnel) {
        this.assignedPersonnel = assignedPersonnel;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public List<EquipmentChecklist> getChecklists() {
        return checklists;
    }

    public void setChecklists(List<EquipmentChecklist> checklists) {
        this.checklists = checklists;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public List<String> getAssignedEquipmentIds() {
        return assignedEquipmentIds;
    }

    public void setAssignedEquipmentIds(List<String> assignedEquipmentIds) {
        this.assignedEquipmentIds = assignedEquipmentIds;
    }
}
