package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.EventApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApprovalRepository extends JpaRepository<EventApproval, Long> {
}
