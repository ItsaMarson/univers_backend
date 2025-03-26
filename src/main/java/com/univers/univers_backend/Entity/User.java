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
    private String firstname;
    private String lastname;
    private String id_number;
    private String phone_number;
    private Boolean emailVerified;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    
    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, String role, String firstname, String lastname, String id_number, String phone_number, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.firstname = firstname;
        this.lastname = lastname;
        this.id_number = id_number;
        this.phone_number = phone_number;
        this.emailVerified = emailVerified;
    }

    public long getId() {return id;}
    public void setId(int id) {this.id = id;}
    public String getEmail() {return this.email;}
    public void setEmail(String email) {this.email = email;}
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getFirstname() {return firstname;}
    public void setFirstname(String firstname) {this.firstname = firstname;}
    public String getLastname() {return lastname;}
    public void setLastname(String lastname) {this.lastname = lastname;}
    public String getId_number() {return id_number;}
    public void setId_number(String id_number) {this.id_number = id_number;}
    public String getPhone_number() {return phone_number;}
    public void setPhone_number(String phone_number) {this.phone_number = phone_number;}
    public Boolean getEmailVerified() {return emailVerified;}
    public void setEmailVerified(Boolean emailVerified) {this.emailVerified = emailVerified;}

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

//    @Override
//    public String toString() {
//        return "UserAuthenticationEntity [email=" + email + ", emailVerified=" + emailVerified + ", firstname=" + firstname
//                + ", id=" + id + ", id_number=" + id_number + ", lastname=" + lastname + ", password=" + password
//                + ", phone_number=" + phone_number + ", role=" + role + "]";
//    }
}