/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository
        extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByPublicId(UUID publicId);

    List<Event> findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Instant endTime, Instant startTime);

    @Query(
            "SELECT e FROM Event e WHERE e.eventVenue.id = :venueId "
                    + "AND e.status <> 'CANCELED' "
                    + "AND e.startTime <= :endTime AND e.endTime >= :startTime")
    List<Event> findConflictingEvents(
            @Param("venueId") Long venueId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query(
            "SELECT e FROM Event e WHERE e.status = 'PENDING' AND e.eventVenue.venueOwner ="
                    + " :venueOwner")
    List<Event> findPendingEventsForVenueOwner(@Param("venueOwner") User venueOwner);

    @Query(
            "SELECT e FROM Event e WHERE e.status = 'PENDING' AND e.organizer.department.deptHead ="
                    + " :deptHead")
    List<Event> findPendingEventsForDeptHead(@Param("deptHead") User deptHead);

    List<Event> findByOrganizer(User organizer);

    List<Event> findByStatus(Status status);

    List<Event> findByStatusAndEventVenue_PublicId(Status status, UUID venuePublicId);

    // For ONGOING status: status is APPROVED, current time is after startTime and before endTime
    List<Event> findByStatusAndStartTimeBeforeAndEndTimeAfter(
            Status status, Instant currentTime, Instant currentTimeCopy);

    // For COMPLETED status: status is ONGOING, current time is after endTime
    List<Event> findByStatusAndEndTimeBefore(Status status, Instant currentTime);

    List<Event> findByStatusInAndEventVenue_PublicId(List<Status> statuses, UUID venuePublicId);

    // Methods for new search endpoint are now removed, will be handled by Specifications
    // List<Event> findByOrganizerAndStatus(User organizer, Status status);
    // List<Event> findAllByStatus(Status status);
    // @Query(
    //         "SELECT e FROM Event e WHERE e.eventVenue.venueOwner = :venueOwner AND (:status is
    // null OR e.status = :status)")
    // List<Event> findRelatedToVenueOwnerByStatusOptional(@Param("venueOwner") User venueOwner,
    // @Param("status") Status status);
    // @Query(
    //         "SELECT e FROM Event e WHERE e.organizer.department.deptHead = :deptHead AND (:status
    // is null OR e.status = :status)")
    // List<Event> findRelatedToDeptHeadByStatusOptional(@Param("deptHead") User deptHead,
    // @Param("status") Status status);
}
