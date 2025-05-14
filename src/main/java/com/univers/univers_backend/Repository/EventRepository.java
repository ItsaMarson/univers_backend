/* (C)2025 */
package com.univers.univers_backend.Repository;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Entity.Venue;
import com.univers.univers_backend.Enum.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
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

    List<Event> findByEventVenueAndStatusAndEndTimeAfterAndStartTimeBefore(
            Venue venue, Status status, Instant startTime, Instant endTime);

    List<Event> findByEventVenue_PublicIdAndStatusAndEndTimeAfterAndStartTimeBefore(
            UUID venuePublicId, Status status, Instant startTime, Instant endTime);

    List<Event> findByDepartment_PublicIdAndStatusAndEndTimeAfterAndStartTimeBefore(
            UUID departmentPublicId, Status status, Instant startTime, Instant endTime);

    List<Event> findByStartTimeBetween(Instant start, Instant end);

    List<Event> findByOrganizer_PublicId(UUID organizerPublicId);

    @Query(
            "SELECT e FROM Event e "
                    + "WHERE e.status = :status "
                    + "AND e.startTime >= :startOfDay AND e.endTime <= :endOfDay")
    List<Event> findApprovedEventsForDate(
            @Param("status") Status status,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay);

    @Query(
            "SELECT e.eventVenue.name AS venueName, COUNT(e) AS eventCount "
                    + "FROM Event e "
                    + "WHERE e.startTime >= :startDate AND e.startTime <= :endDate "
                    + "GROUP BY e.eventVenue.name "
                    + "ORDER BY eventCount DESC")
    List<Object[]> findTopVenuesByEventCount(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query(
            "SELECT FUNCTION('DATE', e.startTime) AS eventDate, COUNT(e) AS dailyEventCount "
                    + "FROM Event e "
                    + "WHERE e.startTime >= :startDate AND e.startTime < :endDatePlusOne "
                    + "GROUP BY FUNCTION('DATE', e.startTime) "
                    + "ORDER BY eventDate ASC")
    List<Object[]> findEventsOverviewByDate(
            @Param("startDate") Instant startDate, @Param("endDatePlusOne") Instant endDatePlusOne);

    @Query(
            "SELECT FUNCTION('DATE', e.createdAt) AS creationDate, SUM(CASE WHEN e.status ="
                + " com.univers.univers_backend.Enum.Status.CANCELED THEN 1 ELSE 0 END) AS"
                + " canceledCount, COUNT(e) AS totalCreatedCount FROM Event e WHERE e.createdAt >="
                + " :startDate AND e.createdAt < :endDatePlusOne GROUP BY FUNCTION('DATE',"
                + " e.createdAt) ORDER BY creationDate ASC")
    List<Object[]> findDailyCancellationStats(
            @Param("startDate") Instant startDate, @Param("endDatePlusOne") Instant endDatePlusOne);

    @Query(
            "SELECT FUNCTION('HOUR', e.startTime) AS eventHour, COUNT(e) AS hourlyEventCount "
                    + "FROM Event e "
                    + "WHERE e.startTime >= :startDate AND e.startTime < :endDatePlusOne "
                    + "GROUP BY FUNCTION('HOUR', e.startTime) "
                    + "ORDER BY eventHour ASC")
    List<Object[]> findPeakHoursByEventStartTime(
            @Param("startDate") Instant startDate, @Param("endDatePlusOne") Instant endDatePlusOne);

    @Query(
            "SELECT e.organizer AS user, COUNT(e) AS eventCount "
                    + "FROM Event e "
                    + "WHERE e.startTime >= :startDate AND e.startTime < :endDatePlusOne "
                    + "GROUP BY e.organizer "
                    + "ORDER BY eventCount DESC")
    List<Object[]> findUserActivityByEventCount(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            Pageable pageable);

    List<Event> findByStatusAndStartTimeAfter(Status status, Instant startTime, Pageable pageable);

    long countByStatusAndStartTimeBetween(Status status, Instant rangeStart, Instant rangeEnd);

    @Query(
            "SELECT e.eventType, COUNT(e) FROM Event e WHERE e.startTime >= :startDate AND"
                    + " e.startTime < :endDate GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> findEventCountsByEventType(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);
}
