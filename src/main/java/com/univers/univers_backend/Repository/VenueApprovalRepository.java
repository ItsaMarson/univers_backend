/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.VenueApproval;
import com.univers.univers_backend.Entity.VenueReservation;
import com.univers.univers_backend.Enum.Status;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueApprovalRepository extends JpaRepository<VenueApproval, Long> {

    List<VenueApproval> findAllByVenueReservation(VenueReservation venueReservation);

    boolean existsByVenueReservationAndSignedByAndStatus(
            VenueReservation venueReservation, User signedBy, Status status);

    List<VenueApproval> findAllByVenueReservationAndStatus(
            VenueReservation venueReservation, Status status);
}
