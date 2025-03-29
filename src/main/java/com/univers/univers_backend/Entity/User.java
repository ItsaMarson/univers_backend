package com.univers.univers_backend.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;


@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String idNumber;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
    private String phoneNumber;
    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String verificationCode;
    private LocalDateTime verificationCodeExpiration;
    @Enumerated(EnumType.STRING)
    private Role roles;
    
    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, Role roles, String firstName, String lastName, String idNumber, String phoneNumber, Boolean emailVerified, String verificationCode) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.idNumber = idNumber;
        this.phoneNumber = phoneNumber;
        this.emailVerified = emailVerified;
        this.verificationCode = verificationCode;
    }

    public long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getEmail() {return this.email;}
    public void setEmail(String email) {this.email = email;}
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getFirstname() {return firstName;}
    public void setFirstname(String firstName) {this.firstName = firstName;}
    public String getLastname() {return lastName;}
    public void setLastname(String lastName) {this.lastName = lastName;}
    public String getId_number() {return idNumber;}
    public void setId_number(String id_number) {this.idNumber = id_number;}

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPhone_number() {return phoneNumber;}
    public void setPhone_number(String phone_number) {this.phoneNumber = phone_number;}
    public Boolean getEmailVerified() {return emailVerified;}
    public void setEmailVerified(Boolean emailVerified) {this.emailVerified = emailVerified;}

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiration() {
        return verificationCodeExpiration;
    }

    public void setVerificationCodeExpiration(LocalDateTime verificationCodeExpiration) {
        this.verificationCodeExpiration = verificationCodeExpiration;
    }

    public Role getRoles() {
        return roles;
    }

    public void setRoles(Role roles) {
        this.roles = roles;
    }

}