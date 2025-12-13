/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "equipment_reservation",
        indexes = {
            @Index(name = "idx_equipment_reservation_status", columnList = "status"),
            @Index(name = "idx_equipment_reservation_equipment_id", columnList = "equipment_id"),
            @Index(
                    name = "idx_equipment_reservation_requesting_user_id",
                    columnList = "requesting_user_id"),
            @Index(name = "idx_equipment_reservation_event_id", columnList = "event_id"),
            @Index(name = "idx_equipment_reservation_department_id", columnList = "department_id"),
            @Index(name = "idx_equipment_reservation_start_time", columnList = "start_time"),
            @Index(name = "idx_equipment_reservation_end_time", columnList = "end_time"),
            @Index(name = "idx_equipment_reservation_created_at", columnList = "created_at"),
            @Index(
                    name = "idx_equipment_reservation_overlap",
                    columnList = "equipment_id,status,start_time,end_time")
        })
public class EquipmentReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event; // Associated event

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_user_id", nullable = false)
    private User requestingUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "equipmentReservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EquipmentApproval> approvals;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.status = Status.PENDING;
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public User getRequestingUser() {
        return requestingUser;
    }

    public void setRequestingUser(User requestingUser) {
        this.requestingUser = requestingUser;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<EquipmentApproval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<EquipmentApproval> approvals) {
        this.approvals = approvals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getPublicId() {
        return publicId;
    }
}
