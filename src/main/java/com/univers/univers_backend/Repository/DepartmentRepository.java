/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Department;
import com.univers.univers_backend.Entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByPublicId(UUID publicId);

    Optional<Department> findByNameIgnoreCase(String name);

    List<Department> findByPublicIdIn(List<UUID> publicIds);

    Boolean existsByDeptHead(User user);
}
