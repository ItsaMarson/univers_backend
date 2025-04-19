package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventApprovalRepository extends JpaRepository<EventApproval, Long> {
    List<EventApproval> findAllByEvent(Event event);
}
