/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.EquipmentCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, Long> {

    Optional<EquipmentCategory> findByPublicId(UUID publicId);

    Optional<EquipmentCategory> findByName(String name);
}
