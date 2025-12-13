/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "equipment_approval_status",
        indexes = {
            @Index(
                    name = "idx_equipment_approval_reservation_id",
                    columnList = "equipment_reservation_id"),
            @Index(name = "idx_equipment_approval_signed_by", columnList = "signed_by_user_id"),
            @Index(name = "idx_equipment_approval_status", columnList = "status"),
            @Index(name = "idx_equipment_approval_date_signed", columnList = "date_signed")
        })
public class EquipmentApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_reservation_id", nullable = false)
    private EquipmentReservation equipmentReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id", nullable = false)
    private User signedBy; // User who approved/rejected

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // APPROVED, REJECTED

    private String remarks;

    @Column(nullable = false)
    private Instant dateSigned;

    @PrePersist
    protected void onCreate() {
        this.dateSigned = Instant.now();
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EquipmentReservation getEquipmentReservation() {
        return equipmentReservation;
    }

    public void setEquipmentReservation(EquipmentReservation equipmentReservation) {
        this.equipmentReservation = equipmentReservation;
    }

    public User getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(User signedBy) {
        this.signedBy = signedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getDateSigned() {
        return dateSigned;
    }

    public void setDateSigned(Instant dateSigned) {
        this.dateSigned = dateSigned;
    }

    public UUID getPublicId() {
        return publicId;
    }
}
