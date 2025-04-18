package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(LocalDateTime endTime, LocalDateTime startTime);
    @Query("SELECT e FROM Event e WHERE e.eventVenue.id = :venueId " +
            "AND e.status <> 'CANCELED' " +
            "AND e.startTime <= :endTime AND e.endTime >= :startTime")
    List<Event> findConflictingEvents(@Param("venueId") Long venueId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
}
