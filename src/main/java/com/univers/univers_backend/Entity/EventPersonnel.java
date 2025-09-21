package com.univers.univers_backend.Entity;


import com.univers.univers_backend.Enum.Status;
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

    private String name;

    private String phoneNumber;

    private Status status;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @PrePersist
    protected void OnCreate() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    public EventPersonnel() {
    }

    public EventPersonnel(Long id, UUID publicId, String name, String phoneNumber, Status status) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.status = status;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
