/* (C)2025 */
package com.univers.univers_backend.Entity;

import com.univers.univers_backend.Enum.Status;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "serial_no", unique = true)
    private String serialNo;

    private String name;

    private Boolean availability;

    @ManyToOne
    @JoinColumn(name = "equipment_owner")
    private User equipmentOwner;

    private Integer quantity;

    private String brand;

    private String imagePath;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "equipment_x_category",
            joinColumns = @JoinColumn(name = "equipment_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<EquipmentCategory> categories = new HashSet<>();

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

    public Equipment() {}

    public Equipment(
            Long id,
            String serialNo,
            String name,
            Boolean availability,
            User equipmentOwner,
            Integer quantity,
            String brand,
            String imagePath,
            Status status,
            Set<EquipmentCategory> categories,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.serialNo = serialNo;
        this.name = name;
        this.availability = availability;
        this.equipmentOwner = equipmentOwner;
        this.quantity = quantity;
        this.brand = brand;
        this.imagePath = imagePath;
        this.status = status;
        this.categories = categories;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {return id;
    }

    public void setId(Long id) {this.id = id;
    }

    public String getSerialNo() {return serialNo;
    }

    public void setSerialNo(String serialNo) {this.serialNo = serialNo;
    }

    public String getName() {return name;
    }

    public void setName(String name) {this.name = name;
    }

    public Boolean getAvailability() {return availability;
    }

    public void setAvailability(Boolean availability) {this.availability = availability;
    }

    public User getEquipmentOwner() {return equipmentOwner;
    }

    public void setEquipmentOwner(User equipmentOwner) {this.equipmentOwner = equipmentOwner;
    }

    public Integer getQuantity() {return quantity;
    }

    public void setQuantity(Integer quantity) {this.quantity = quantity;
    }

    public String getBrand() {return brand;
    }

    public void setBrand(String brand) {this.brand = brand;
    }

    public Status getStatus() {return status;
    }

    public void setStatus(Status status) {this.status = status;
    }

    public String getImagePath() {return imagePath;
    }

    public void setImagePath(String imagePath) {this.imagePath = imagePath;
    }

    public Instant getCreatedAt() {return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {this.updatedAt = updatedAt;
    }

    public UUID getPublicId() {return publicId;
    }

    public Set<EquipmentCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<EquipmentCategory> categories) {
        this.categories = categories;
    }

    public void addCategory(EquipmentCategory category) {
        this.categories.add(category);
        category.getEquipments().add(this);
    }

    public void removeCategory(EquipmentCategory category) {
        this.categories.remove(category);
        category.getEquipments().remove(this);
    }
}
