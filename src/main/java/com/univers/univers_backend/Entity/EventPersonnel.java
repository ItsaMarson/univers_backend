package com.univers.univers_backend.Entity;


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

    public EventPersonnel(Long id, UUID publicId, String name) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
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
}
