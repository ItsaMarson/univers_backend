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
                    + "AND e.status IN ('PENDING', 'APPROVED', 'ONGOING') "
                    + "AND e.startTime <= :endTime AND e.endTime >= :startTime")
    List<Event> findConflictingEvents(
            @Param("venueId") Long venueId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query(
            "SELECT e FROM Event e WHERE e.eventVenue.id = :venueId "
                    + "AND e.status IN ('PENDING', 'APPROVED', 'ONGOING') "
                    + "AND e.id <> :excludeEventId "
                    + "AND e.startTime <= :endTime AND e.endTime >= :startTime")
    List<Event> findConflictingEventsExcludingCurrent(
            @Param("venueId") Long venueId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeEventId") Long excludeEventId);

    List<Event> findByOrganizer(User organizer);

    List<Event> findByStatus(Status status);

    List<Event> findByStatusAndEventVenue_PublicId(Status status, UUID venuePublicId);

    List<Event> findByStatusAndStartTimeBeforeAndEndTimeAfter(
            Status status, Instant currentTime, Instant currentTimeCopy);

    List<Event> findByStatusAndEndTimeBefore(Status status, Instant currentTime);

    List<Event> findByStatusInAndEventVenue_PublicId(List<Status> statuses, UUID venuePublicId);

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
            "SELECT e.eventVenue.name AS venueName, e.status AS eventStatus, COUNT(e) AS"
                + " statusCount FROM Event e WHERE e.startTime >= :startDate AND e.startTime <"
                + " :endDatePlusOne GROUP BY e.eventVenue.name, e.status ORDER BY venueName ASC,"
                + " eventStatus ASC")
    List<Object[]> findTopVenuesByEventCount(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            Pageable pageable);

    @Query(
            "SELECT FUNCTION('DATE', e.startTime) AS eventDate, e.status AS eventStatus, COUNT(e)"
                + " AS statusCount FROM Event e WHERE e.startTime >= :startDate AND e.startTime <"
                + " :endDatePlusOne GROUP BY FUNCTION('DATE', e.startTime), e.status ORDER BY"
                + " eventDate ASC, eventStatus ASC")
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
            "SELECT e.eventType, e.status, COUNT(e) FROM Event e "
                    + "WHERE e.startTime >= :startDate AND e.startTime < :endDatePlusOne "
                    + "GROUP BY e.eventType, e.status "
                    + "ORDER BY e.eventType ASC, e.status ASC")
    List<Object[]> findEventCountsByEventType(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            Pageable pageable);

    @Query(
            "SELECT e.department.publicId, e.department.name, e.status, COUNT(e) "
                    + "FROM Event e "
                    + "WHERE e.department IS NOT NULL "
                    + "AND e.startTime >= :startDate AND e.startTime < :endDatePlusOne "
                    + "GROUP BY e.department.publicId, e.department.name, e.status "
                    + "ORDER BY e.department.name ASC, e.status ASC")
    List<Object[]> findTopDepartmentsByEventCount(
            @Param("startDate") Instant startDate,
            @Param("endDatePlusOne") Instant endDatePlusOne,
            Pageable pageable);
}
