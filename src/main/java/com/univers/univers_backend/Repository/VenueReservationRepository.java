/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Entity.VenueReservation;
import com.univers.univers_backend.Enum.Status;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueReservationRepository extends JpaRepository<VenueReservation, Long> {

    // Find conflicting reservations for a given venue and time range, excluding canceled ones
    @Query(
            "SELECT vr FROM VenueReservation vr WHERE vr.venue.id = :venueId "
                    + "AND vr.status <> com.univers.univers_backend.Enum.Status.CANCELED "
                    + "AND vr.startTime < :endTime AND vr.endTime > :startTime")
    List<VenueReservation> findConflictingReservations(
            @Param("venueId") Long venueId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    List<VenueReservation> findByVenue(Venue venue);

    List<VenueReservation> findByStatus(Status status);

    List<VenueReservation> findByRequestingUser(User requestingUser);

    // Add more queries as needed, e.g., find by requesting user, find pending for venue owner
    @Query(
            "SELECT vr FROM VenueReservation vr WHERE vr.status = 'PENDING' AND"
                    + " vr.venue.venueOwner.id = :venueOwnerId")
    List<VenueReservation> findPendingReservationsForVenueOwner(
            @Param("venueOwnerId") Long venueOwnerId);
}
