package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByEquipmentOwner(User owner);
}
