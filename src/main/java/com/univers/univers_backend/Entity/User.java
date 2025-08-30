/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Email;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Email(message = "Invalid Email format")
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    @Column(unique = true, nullable = false)
    private String idNumber;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    private String phoneNumber;

    private String telephoneNumber;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String verificationCode;
    private Instant verificationCodeExpiration;

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private Boolean active;

    public String profileImagePath;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public User() {}

    public User(
            String email,
            String password,
            Set<Role> roles,
            String firstName,
            String lastName,
            String idNumber,
            String phoneNumber,
            String telephoneNumber,
            Boolean emailVerified,
            String verificationCode,
            Boolean active) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.idNumber = idNumber;
        this.phoneNumber = phoneNumber;
        this.telephoneNumber = telephoneNumber;
        this.emailVerified = emailVerified;
        this.verificationCode = verificationCode;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstName;
    }

    public void setFirstname(String firstName) {
        this.firstName = firstName;
    }

    public String getLastname() {
        return lastName;
    }

    public void setLastname(String lastName) {
        this.lastName = lastName;
    }

    public String getId_number() {
        return idNumber;
    }

    public void setId_number(String id_number) {
        this.idNumber = id_number;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPhone_number() {
        return phoneNumber;
    }

    public void setPhone_number(String phone_number) {
        this.phoneNumber = phone_number;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Instant getVerificationCodeExpiration() {
        return verificationCodeExpiration;
    }

    public void setVerificationCodeExpiration(Instant verificationCodeExpiration) {
        this.verificationCodeExpiration = verificationCodeExpiration;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public String getFullName() {
        return getFirstname() + " " + getLastname();
    }

    public UUID getPublicId() {
        return publicId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.roles == null || this.roles.isEmpty()) {
            return Collections.emptyList();
        }
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active != null && this.active;
    }
}
