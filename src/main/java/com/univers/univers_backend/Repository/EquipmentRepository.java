/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Equipment;
import com.univers.univers_backend.Entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByPublicId(UUID publicId);

    List<Equipment> findAllByEquipmentOwner(User owner);
}
