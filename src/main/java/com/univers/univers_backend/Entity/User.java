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
    private String id_number;
    private String phone_number;
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

    public User(String email, String password, Set<Role> roles, String firstname, String lastname, String id_number, String phone_number, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.firstName = firstname;
        this.lastName = lastname;
        this.roles = roles;
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
    public String getFirstname() {return firstName;}
    public void setFirstname(String firstname) {this.firstName = firstname;}
    public String getLastname() {return lastName;}
    public void setLastname(String lastname) {this.lastName = lastname;}
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