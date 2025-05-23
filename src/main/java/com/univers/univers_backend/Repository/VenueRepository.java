/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByPublicId(UUID publicId);

    Optional<Venue> findByNameIgnoreCase(String name);

    Optional<Venue> findByVenueOwner(User venueOwner);

    List<Venue> findAllByPublicIdIn(List<UUID> publicIds);
}
