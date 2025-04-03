package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue,Long> {
    Optional<Venue> findByNameIgnoreCase(String name);
}
