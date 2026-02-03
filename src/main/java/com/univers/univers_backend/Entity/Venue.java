/* (C)2025 */
package com.univers.univers_backend.Entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "venue",
        indexes = {
            @Index(name = "idx_venue_owner_id", columnList = "venue_owner_id"),
            @Index(name = "idx_venue_name", columnList = "name")
        })
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private String name;

    private String location;

    @ManyToOne
    @JoinColumn(name = "venue_owner_id", nullable = true)
    private User venueOwner;

    private String imagePath;

    @Column(updatable = false)
    Instant createdAt;

    Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Venue() {}

    public Venue(
            Long venueId,
            String venueName,
            String venueLocation,
            User owner,
            String imgPath,
            Instant createdTime,
            Instant updatedTime) {
        this.id = venueId;
        this.name = venueName;
        this.location = venueLocation;
        this.venueOwner = owner;
        this.imagePath = imgPath;
        this.createdAt = createdTime;
        this.updatedAt = updatedTime;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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
