/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApprovalRepository extends JpaRepository<EventApproval, Long> {
    Optional<EventApproval> findByPublicId(UUID publicId);

    List<EventApproval> findAllByEvent(Event event);

    boolean existsByEventAndSignedByAndStatus(Event event, User signedBy, Status status);

    List<EventApproval> findAllByEventAndStatus(Event event, Status status);

    Optional<EventApproval> findByEventAndSignedBy(Event event, User signedBy);
}
