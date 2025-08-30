/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    private String eventName;
    private String eventType;
    private Instant startTime;
    private Instant endTime;
    private String approvedLetterPath;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue eventVenue;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private List<String> assignedPersonnel;

    @PrePersist
    protected void OnCreate() {
        this.createdAt = Instant.now();
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventApproval> approvals;

    private String imagePath;

    public Event() {}

    public Event(
            Long id,
            String eventName,
            String eventType,
            String status,
            User organizer,
            Venue eventVenue,
            Instant startTime,
            Instant endTime,
            String approvedLetterPath,
            String imagePath,
            Instant createdAt,
            Instant updatedAt,
            List<String> assignedPersonnel) {
        this.id = id;
        this.eventName = eventName;
        this.eventType = eventType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.organizer = organizer;
        this.eventVenue = eventVenue;
        this.imagePath = imagePath;
        this.approvedLetterPath = approvedLetterPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.assignedPersonnel = assignedPersonnel;

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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getApprovedLetterPath() {
        return approvedLetterPath;
    }

    public void setApprovedLetterPath(String approvedLetterPath) {
        this.approvedLetterPath = approvedLetterPath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public List<EventApproval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<EventApproval> approvals) { //Subject to deletion, due to unused
        this.approvals = approvals;
    }

    public List<String> getAssignedPersonnel() {
        return assignedPersonnel;
    }

    public void setAssignedPersonnel(List<String> assignedPersonnel) {
        this.assignedPersonnel = assignedPersonnel;
    }
}
