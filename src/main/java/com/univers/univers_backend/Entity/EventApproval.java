/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_approval_status")
public class EventApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id")
    private User signedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    private String remarks;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime dateSigned;

    public EventApproval() {}

    @PrePersist
    protected void onCreate() {
        if (this.dateSigned == null) {
            this.dateSigned = LocalDateTime.now();
        }
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    public EventApproval(
            Long approvalId,
            User signedByUser,
            Event relatedEvent,
            String approvalRemarks,
            Status approvalStatus,
            LocalDateTime approvalDateSigned) {
        this.id = approvalId;
        this.signedBy = signedByUser;
        this.event = relatedEvent;
        this.remarks = approvalRemarks;
        this.status = approvalStatus;
        this.dateSigned = approvalDateSigned;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(User signedBy) {
        this.signedBy = signedBy;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getDateSigned() {
        return dateSigned;
    }

    public void setDateSigned(LocalDateTime dateSigned) {
        this.dateSigned = dateSigned;
    }

    public UUID getPublicId() {
        return publicId;
    }
}
