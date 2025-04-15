package com.univers.univers_backend.Entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;
    private String eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String approvedLetter;

    private String status;
    @ManyToOne
    @JoinColumn(name = "organizer_id", unique = true)
    private User organizer;

    @ManyToOne
    @JoinColumn(name = "venue_id", unique = true)
    private Venue eventVenue;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void OnCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public Event(){}

    public Event(Long id, String eventName, String eventType, String status, User organizer, Venue eventVenue, LocalDateTime startTime, LocalDateTime endTime, String approvedLetter, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.eventName = eventName;
        this.eventType = eventType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizer = organizer;
        this.eventVenue = eventVenue;
        this.approvedLetter = approvedLetter;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public User getOrganizer() {
        return organizer;
    }

    public void setOrganizer(User organizer) {
        this.organizer = organizer;
    }

    public Venue getEventVenue() {
        return eventVenue;
    }

    public void setEventVenue(Venue eventVenue) {
        this.eventVenue = eventVenue;
    }

    public String getApprovedLetter() {
        return approvedLetter;
    }

    public void setApprovedLetter(String approvedLetter) {
        this.approvedLetter = approvedLetter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
