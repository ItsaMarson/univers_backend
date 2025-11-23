/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Enum.Task;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "event_personnel")
public class EventPersonnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_personnel_id", nullable = false)
    private User assignedPersonnel;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    public EventPersonnel(
            Long id,
            UUID publicId,
            User assignedPersonnel,
            String phoneNumber,
            Status status,
            Task task) {
        this.id = id;
        this.publicId = publicId;
        this.assignedPersonnel = assignedPersonnel;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.task = task;
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
}
