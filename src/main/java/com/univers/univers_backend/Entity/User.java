package com.univers.univers_backend.Entity;

import jakarta.persistence.*;

import java.util.Set;


@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private int id;

    
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String idNumber;
    private String phoneNumber;
    @Column(nullable = false)
    private Boolean emailVerified = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    
    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, Set<Role> roles, String firstName, String lastName, String idNumber, String phoneNumber, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.idNumber = idNumber;
        this.phoneNumber = phoneNumber;
        this.emailVerified = emailVerified;
    }

    public long getId() {return id;}
    public void setId(int id) {this.id = id;}
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
    public String getPhone_number() {return phoneNumber;}
    public void setPhone_number(String phone_number) {this.phoneNumber = phone_number;}
    public Boolean getEmailVerified() {return emailVerified;}
    public void setEmailVerified(Boolean emailVerified) {this.emailVerified = emailVerified;}

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

}