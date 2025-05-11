/* (C)2025 */
package com.univers.univers_backend.Entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    private String name;

    private String description;

    @OneToOne
    @JoinColumn(name = "dept_head_id", unique = true)
    private User deptHead;

    Instant createdAt;
    Instant updatedAt;

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

    public Department() {}

    public Department(
            Long id,
            String name,
            String description,
            User deptHead,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.deptHead = deptHead;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getDeptHead() {
        return deptHead;
    }

    public void setDeptHead(User deptHead) {
        this.deptHead = deptHead;
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
