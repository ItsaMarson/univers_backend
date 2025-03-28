package com.univers.univers_backend.Entity;

import jakarta.persistence.*;


@Entity
@Table(name = "departments")
public class Department {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    private String name;

    private String location;
    private String description;

    @OneToOne
    @JoinColumn(name = "dept_head_id", unique = true)
    private User deptHead;


    public Department(Long id, String name, String location, String description, User deptHead) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.deptHead = deptHead;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
}
