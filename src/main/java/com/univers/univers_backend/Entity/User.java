package com.univers.univers_backend.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;



@Entity
@Table(name = "user_authentication")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private int id;

    
    private String email;
    private String password;
    private String role;
    private String firstname;
    private String lastname;
    private String id_number;
    private String phone_number;
    private Boolean emailVerified;
    

    
    public User() {
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(String email, String password, String role, String firstname, String lastname, String id_number, String phone_number, Boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.role = role;
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
    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}
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


    @Override
    public String toString() {
        return "UserAuthenticationEntity [email=" + email + ", emailVerified=" + emailVerified + ", firstname=" + firstname
                + ", id=" + id + ", id_number=" + id_number + ", lastname=" + lastname + ", password=" + password
                + ", phone_number=" + phone_number + ", role=" + role + "]";
    }
}