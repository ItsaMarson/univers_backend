package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
