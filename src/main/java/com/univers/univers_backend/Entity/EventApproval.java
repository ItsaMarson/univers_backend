package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_approval_status")
public class EventApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public EventApproval() { }

    public EventApproval(Long id, User signedBy, Event event, String remarks, Status status, LocalDateTime dateSigned) {
        this.id = id;
        this.signedBy = signedBy;
        this.event = event;
        this.remarks = remarks;
        this.status = status;
        this.dateSigned = dateSigned;
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
}
