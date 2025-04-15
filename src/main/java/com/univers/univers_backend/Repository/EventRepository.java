package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
