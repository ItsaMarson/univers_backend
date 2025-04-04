package com.univers.univers_backend.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "venue")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    @ManyToOne
    @JoinColumn(name = "venue_owner_id", nullable = false)
    private User venueOwner;

    @Column(updatable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Venue(){}
    public Venue(Long id, String name, String location, User venueOwner, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.venueOwner = venueOwner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public User getVenueOwner() {
        return venueOwner;
    }

    public void setVenueOwner(User venueOwner) {
        this.venueOwner = venueOwner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
