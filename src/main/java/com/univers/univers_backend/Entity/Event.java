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

    public Event(Long id, String eventName, String eventType, LocalDateTime startTime, LocalDateTime endTime, String approvedLetter, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.eventName = eventName;
        this.eventType = eventType;
        this.startTime = startTime;
        this.endTime = endTime;
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
