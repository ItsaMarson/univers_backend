/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.ActivityLog;
import com.univers.univers_backend.Entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ActivityLog> findAllByOrderByCreatedAtDesc();

    Page<ActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<ActivityLog> findByActionContainingIgnoreCaseOrderByCreatedAtDesc(
            String action, Pageable pageable);

    Page<ActivityLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    ActivityLog findByPublicId(UUID publicId);
}
