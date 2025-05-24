/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.dashboard.CancellationRateDTO;
import com.univers.univers_backend.DTO.dashboard.EventCountDTO;
import com.univers.univers_backend.DTO.dashboard.EventTypeSummaryDTO;
import com.univers.univers_backend.DTO.dashboard.PeakHourDTO;
import com.univers.univers_backend.DTO.dashboard.RecentActivityItemDTO;
import com.univers.univers_backend.DTO.dashboard.TopEquipmentDTO;
import com.univers.univers_backend.DTO.dashboard.TopVenueDTO;
import com.univers.univers_backend.DTO.dashboard.UserActivityDTO;
import com.univers.univers_backend.DTO.dashboard.UserReservationActivityDTO;
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.EventMapper;
import com.univers.univers_backend.Repository.EquipmentReservationRepository;
import com.univers.univers_backend.Repository.EventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardService {

    private final EventRepository eventRepository;
    private final EquipmentReservationRepository equipmentReservationRepository;
    private final EventMapper eventMapper;

    public DashboardService(
            EventRepository eventRepository,
            EquipmentReservationRepository equipmentReservationRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.equipmentReservationRepository = equipmentReservationRepository;
        this.eventMapper = eventMapper;
    }

    public List<TopVenueDTO> getTopVenues(LocalDate startDate, LocalDate endDate, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = Pageable.unpaged();

        List<Object[]> results =
                eventRepository.findTopVenuesByEventCount(
                        startInstant, endInstantPlusOne, pageable);

        if (results == null) return new ArrayList<>();

        Map<String, TopVenueDTO> topVenuesMap = new HashMap<>();

        for (Object[] result : results) {
            String venueName = (String) result[0];
            Status eventStatus = (Status) result[1];
            Long count = (Long) result[2];

            if (venueName == null) continue;

            TopVenueDTO dto =
                    topVenuesMap.computeIfAbsent(
                            venueName, k -> new TopVenueDTO(k, 0L, 0L, 0L, 0L, 0L, 0L, 0L));

            long newTotalCount = dto.totalEventCount() + count;
            long newApprovedCount = dto.approvedCount();
            long newPendingCount = dto.pendingCount();
            long newCanceledCount = dto.canceledCount();
            long newRejectedCount = dto.rejectedCount();
            long newOngoingCount = dto.ongoingCount();
            long newCompletedCount = dto.completedCount();

            switch (eventStatus) {
                case APPROVED:
                    newApprovedCount += count;
                    break;
                case PENDING:
                    newPendingCount += count;
                    break;
                case CANCELED:
                    newCanceledCount += count;
                    break;
                case REJECTED:
                    newRejectedCount += count;
                    break;
                case ONGOING:
                    newOngoingCount += count;
                    break;
                case COMPLETED:
                    newCompletedCount += count;
                    break;
                default:
                    break;
            }
            topVenuesMap.put(
                    venueName,
                    new TopVenueDTO(
                            venueName,
                            newTotalCount,
                            newApprovedCount,
                            newPendingCount,
                            newCanceledCount,
                            newRejectedCount,
                            newOngoingCount,
                            newCompletedCount));
        }

        return topVenuesMap.values().stream()
                .sorted(Comparator.comparing(TopVenueDTO::totalEventCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<TopEquipmentDTO> getTopEquipment(
            LocalDate startDate, LocalDate endDate, String equipmentTypeName, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = Pageable.unpaged();

        String filterTypeName = StringUtils.hasText(equipmentTypeName) ? equipmentTypeName : null;

        List<Object[]> results =
                equipmentReservationRepository.findTopEquipmentByReservationCount(
                        startInstant, endInstantPlusOne, filterTypeName, pageable);

        if (results == null) return new ArrayList<>();

        Map<String, TopEquipmentDTO> topEquipmentMap = new HashMap<>();

        for (Object[] result : results) {
            String equipmentName = (String) result[0];
            Status reservationStatus = (Status) result[1];
            Long count = (Long) result[2];

            if (equipmentName == null) continue;

            TopEquipmentDTO dto =
                    topEquipmentMap.computeIfAbsent(
                            equipmentName,
                            k ->
                                    new TopEquipmentDTO(
                                            k, 0L, 0L, 0L, 0L, 0L, 0L,
                                            0L) // name, total, pending, approved, rejected,
                            // canceled, pickedUp, returned
                            );

            long newTotalReservationCount = dto.totalReservationCount() + count;
            long newPendingCount = dto.pendingCount();
            long newApprovedCount = dto.approvedCount();
            long newRejectedCount = dto.rejectedCount();
            long newCanceledCount = dto.canceledCount();
            long newOngoingCount = dto.ongoingCount();
            long newCompletedCount = dto.completedCount();

            switch (reservationStatus) {
                case PENDING:
                    newPendingCount += count;
                    break;
                case APPROVED:
                    newApprovedCount += count;
                    break;
                case REJECTED:
                    newRejectedCount += count;
                    break;
                case CANCELED:
                    newCanceledCount += count;
                    break;
                case ONGOING:
                    newOngoingCount += count;
                    break;
                case COMPLETED:
                    newCompletedCount += count;
                    break;
                default:
                    break;
            }
            topEquipmentMap.put(
                    equipmentName,
                    new TopEquipmentDTO(
                            equipmentName,
                            newTotalReservationCount,
                            newPendingCount,
                            newApprovedCount,
                            newRejectedCount,
                            newCanceledCount,
                            newOngoingCount,
                            newCompletedCount));
        }

        return topEquipmentMap.values().stream()
                .sorted(Comparator.comparing(TopEquipmentDTO::totalReservationCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<UserReservationActivityDTO> getUserReservationActivity(
            LocalDate startDate, LocalDate endDate, String userFilter, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = Pageable.unpaged();

        List<Object[]> results =
                equipmentReservationRepository.findUserReservationActivityByCount(
                        startInstant, endInstantPlusOne, userFilter, pageable);

        if (results == null) return new ArrayList<>();

        Map<UUID, UserReservationActivityDTO> userActivityMap = new HashMap<>();

        for (Object[] result : results) {
            UUID userPublicId = (UUID) result[0];
            String userFirstName = (String) result[1];
            String userLastName = (String) result[2];
            Status reservationStatus = (Status) result[3];
            Long count = (Long) result[4];

            if (userPublicId == null) continue;

            UserReservationActivityDTO dto =
                    userActivityMap.computeIfAbsent(
                            userPublicId,
                            k ->
                                    new UserReservationActivityDTO(
                                            k,
                                            userFirstName,
                                            userLastName,
                                            0L,
                                            0L,
                                            0L,
                                            0L,
                                            0L,
                                            0L,
                                            0L));

            long newTotalReservationCount = dto.totalReservationCount() + count;
            long newPendingCount = dto.pendingCount();
            long newApprovedCount = dto.approvedCount();
            long newRejectedCount = dto.rejectedCount();
            long newCanceledCount = dto.canceledCount();
            long newOngoingCount = dto.ongoingCount();
            long newCompletedCount = dto.completedCount();

            switch (reservationStatus) {
                case PENDING:
                    newPendingCount += count;
                    break;
                case APPROVED:
                    newApprovedCount += count;
                    break;
                case REJECTED:
                    newRejectedCount += count;
                    break;
                case CANCELED:
                    newCanceledCount += count;
                    break;
                case ONGOING:
                    newOngoingCount += count;
                    break;
                case COMPLETED:
                    newCompletedCount += count;
                    break;
                default:
                    break;
            }
            userActivityMap.put(
                    userPublicId,
                    new UserReservationActivityDTO(
                            userPublicId,
                            userFirstName,
                            userLastName,
                            newTotalReservationCount,
                            newPendingCount,
                            newApprovedCount,
                            newRejectedCount,
                            newCanceledCount,
                            newOngoingCount,
                            newCompletedCount));
        }

        return userActivityMap.values().stream()
                .sorted(
                        Comparator.comparing(UserReservationActivityDTO::totalReservationCount)
                                .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<EventCountDTO> getEventsOverview(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> results =
                eventRepository.findEventsOverviewByDate(startInstant, endInstantPlusOne);

        if (results == null) return new ArrayList<>();

        Map<LocalDate, EventCountDTO> eventCountsByDate = new HashMap<>();

        for (Object[] result : results) {
            LocalDate eventDate;
            if (result[0] instanceof java.sql.Date) {
                eventDate = ((java.sql.Date) result[0]).toLocalDate();
            } else if (result[0] instanceof LocalDate) {
                eventDate = (LocalDate) result[0];
            } else if (result[0] != null) {
                try {
                    eventDate = LocalDate.parse(result[0].toString());
                } catch (Exception e) {
                    System.err.println(
                            "Could not parse date from query result: "
                                    + result[0]
                                    + " Error: "
                                    + e.getMessage());
                    continue;
                }
            } else {
                continue;
            }

            Status eventStatus = (Status) result[1];
            long count = (Long) result[2];

            EventCountDTO dto =
                    eventCountsByDate.computeIfAbsent(
                            eventDate, k -> new EventCountDTO(k, 0, 0, 0, 0, 0, 0));

            EventCountDTO updatedDto =
                    switch (eventStatus) {
                        case APPROVED ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount() + count,
                                        dto.pendingCount(),
                                        dto.canceledCount(),
                                        dto.rejectedCount(),
                                        dto.ongoingCount(),
                                        dto.completedCount());
                        case PENDING ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount(),
                                        dto.pendingCount() + count,
                                        dto.canceledCount(),
                                        dto.rejectedCount(),
                                        dto.ongoingCount(),
                                        dto.completedCount());
                        case CANCELED ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount(),
                                        dto.pendingCount(),
                                        dto.canceledCount() + count,
                                        dto.rejectedCount(),
                                        dto.ongoingCount(),
                                        dto.completedCount());
                        case REJECTED ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount(),
                                        dto.pendingCount(),
                                        dto.canceledCount(),
                                        dto.rejectedCount() + count,
                                        dto.ongoingCount(),
                                        dto.completedCount());
                        case ONGOING ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount(),
                                        dto.pendingCount(),
                                        dto.canceledCount(),
                                        dto.rejectedCount(),
                                        dto.ongoingCount() + count,
                                        dto.completedCount());
                        case COMPLETED ->
                                new EventCountDTO(
                                        dto.date(),
                                        dto.approvedCount(),
                                        dto.pendingCount(),
                                        dto.canceledCount(),
                                        dto.rejectedCount(),
                                        dto.ongoingCount(),
                                        dto.completedCount() + count);
                        default -> dto;
                    };
            eventCountsByDate.put(eventDate, updatedDto);
        }

        return eventCountsByDate.values().stream()
                .sorted(Comparator.comparing(EventCountDTO::date))
                .collect(Collectors.toList());
    }

    public List<CancellationRateDTO> getCancellationRates(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> results =
                eventRepository.findDailyCancellationStats(startInstant, endInstantPlusOne);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(
                        result -> {
                            LocalDate creationDate;
                            if (result[0] instanceof java.sql.Date) {
                                creationDate = ((java.sql.Date) result[0]).toLocalDate();
                            } else if (result[0] instanceof LocalDate) {
                                creationDate = (LocalDate) result[0];
                            } else if (result[0] != null) {
                                try {
                                    creationDate = LocalDate.parse(result[0].toString());
                                } catch (Exception e) {
                                    System.err.println(
                                            "Could not parse date from cancellation stats query"
                                                    + " result: "
                                                    + result[0]
                                                    + " Error: "
                                                    + e.getMessage());
                                    return null;
                                }
                            } else {
                                return null;
                            }

                            long canceledCount = ((Number) result[1]).longValue();
                            long totalCreatedCount = ((Number) result[2]).longValue();

                            double rate = 0.0;
                            if (totalCreatedCount > 0) {
                                rate = ((double) canceledCount / totalCreatedCount) * 100.0;
                            }

                            rate = Math.round(rate * 100.0) / 100.0;

                            return new CancellationRateDTO(
                                    creationDate, rate, canceledCount, totalCreatedCount);
                        })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<PeakHourDTO> getPeakReservationHours(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> results =
                eventRepository.findPeakHoursByEventStartTime(startInstant, endInstantPlusOne);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(
                        result -> {
                            Integer hourOfDay = null;
                            if (result[0] instanceof Number) {
                                hourOfDay = ((Number) result[0]).intValue();
                            } else if (result[0] != null) {
                                try {
                                    hourOfDay = Integer.parseInt(result[0].toString());
                                } catch (NumberFormatException e) {
                                    System.err.println(
                                            "Could not parse hour from peak hours query result: "
                                                    + result[0]
                                                    + " Error: "
                                                    + e.getMessage());
                                    return null;
                                }
                            }
                            if (hourOfDay == null) return null;

                            Long count = (Long) result[1];
                            return new PeakHourDTO(hourOfDay, count);
                        })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getUserActivity(
            LocalDate startDate, LocalDate endDate, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> results =
                eventRepository.findUserActivityByEventCount(
                        startInstant, endInstantPlusOne, pageable);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(
                        result -> {
                            User user = (User) result[0];
                            Long eventCount = (Long) result[1];
                            if (user == null) return null;

                            return new UserActivityDTO(
                                    user.getPublicId(),
                                    user.getFirstname(),
                                    user.getLastname(),
                                    eventCount);
                        })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<RecentActivityItemDTO> getRecentActivity(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));

        List<Event> recentEvents = eventRepository.findAll(pageable).getContent();
        List<EquipmentReservation> recentReservations =
                equipmentReservationRepository.findAll(pageable).getContent();

        List<RecentActivityItemDTO> activityItems = new ArrayList<>();

        recentEvents.forEach(
                event -> {
                    Instant eventUpdatedAt = event.getUpdatedAt();
                    Instant eventCreatedAt = event.getCreatedAt();
                    Instant activityTimestamp;
                    String description;
                    String venueName =
                            event.getEventVenue() != null ? event.getEventVenue().getName() : "N/A";

                    if (eventUpdatedAt == null || eventUpdatedAt.equals(eventCreatedAt)) {
                        description =
                                "Event '"
                                        + event.getEventName()
                                        + "' created for venue '"
                                        + venueName
                                        + "'.";
                        activityTimestamp = eventCreatedAt;
                    } else {
                        description =
                                "Event '"
                                        + event.getEventName()
                                        + "' at venue '"
                                        + venueName
                                        + "' status is now "
                                        + event.getStatus().toString().toLowerCase()
                                        + ".";
                        activityTimestamp = eventUpdatedAt;
                    }

                    if (activityTimestamp == null) {
                        activityTimestamp = eventCreatedAt;
                    }
                    if (activityTimestamp == null) {
                        activityTimestamp = Instant.now();
                    }

                    String actorName =
                            event.getOrganizer() != null
                                    ? event.getOrganizer().getFirstname()
                                            + " "
                                            + event.getOrganizer().getLastname()
                                    : "System";
                    String entityPath = event.getPublicId().toString();

                    activityItems.add(
                            new RecentActivityItemDTO(
                                    event.getPublicId().toString(),
                                    "Event",
                                    event.getEventName(),
                                    description,
                                    activityTimestamp,
                                    actorName,
                                    entityPath));
                });

        recentReservations.forEach(
                reservation -> {
                    Instant reservationUpdatedAt = reservation.getUpdatedAt();
                    Instant reservationCreatedAt = reservation.getCreatedAt();
                    Instant activityTimestamp;
                    String description;
                    String eventNameForReservation =
                            reservation.getEvent() != null
                                    ? reservation.getEvent().getEventName()
                                    : "N/A";
                    String equipmentName =
                            reservation.getEquipment() != null
                                    ? reservation.getEquipment().getName()
                                    : "N/A";

                    if (reservationUpdatedAt == null
                            || reservationUpdatedAt.equals(reservationCreatedAt)) {
                        description =
                                "Reservation for '"
                                        + equipmentName
                                        + "' for event '"
                                        + eventNameForReservation
                                        + "' created.";
                        activityTimestamp = reservationCreatedAt;
                    } else {
                        description =
                                "Reservation for '"
                                        + equipmentName
                                        + "' for event '"
                                        + eventNameForReservation
                                        + "' status is now "
                                        + reservation.getStatus().toString().toLowerCase()
                                        + ".";
                        activityTimestamp = reservationUpdatedAt;
                    }

                    if (activityTimestamp == null) {
                        activityTimestamp = reservationCreatedAt;
                    }
                    if (activityTimestamp == null) {
                        activityTimestamp = Instant.now();
                    }

                    String actorName =
                            reservation.getRequestingUser() != null
                                    ? reservation.getRequestingUser().getFirstname()
                                            + " "
                                            + reservation.getRequestingUser().getLastname()
                                    : "System";
                    String entityPath = reservation.getEvent().getPublicId().toString();

                    activityItems.add(
                            new RecentActivityItemDTO(
                                    reservation.getPublicId().toString(),
                                    "Equipment Reservation",
                                    "Reservation for " + reservation.getEquipment().getName(),
                                    description,
                                    activityTimestamp,
                                    actorName,
                                    entityPath));
                });

        return activityItems.stream()
                .sorted(Comparator.comparing(RecentActivityItemDTO::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<EventDTO> getUpcomingApprovedEvents(int limit) {
        Instant now = Instant.now();
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "startTime"));

        List<Event> upcomingEvents =
                eventRepository.findByStatusAndStartTimeAfter(Status.APPROVED, now, pageable);

        return upcomingEvents.stream().map(eventMapper::toDto).collect(Collectors.toList());
    }

    public long getUpcomingApprovedEventCountForNextDays(int days) {
        Instant now = Instant.now();
        Instant futureDate = now.plus(days, java.time.temporal.ChronoUnit.DAYS);
        return eventRepository.countByStatusAndStartTimeBetween(Status.APPROVED, now, futureDate);
    }

    public List<EventTypeSummaryDTO> getEventCountsByEventType(
            LocalDate startDate, LocalDate endDate, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        // Fetch all results first, then sort and limit in service
        Pageable pageable = Pageable.unpaged();

        List<Object[]> results =
                eventRepository.findEventCountsByEventType(
                        startInstant, endInstantPlusOne, pageable);

        if (results == null) return new ArrayList<>();

        Map<String, EventTypeSummaryDTO> summariesByType = new HashMap<>();

        for (Object[] result : results) {
            String eventType = (String) result[0];
            Status eventStatus = (Status) result[1];
            Long count = (Long) result[2];

            if (eventType == null) continue;

            EventTypeSummaryDTO dto =
                    summariesByType.computeIfAbsent(
                            eventType,
                            k ->
                                    new EventTypeSummaryDTO(
                                            k, 0L, 0L, 0L, 0L, 0L, 0L,
                                            0L) // name, total, approved, pending, canceled,
                            // rejected, ongoing, completed
                            );

            long newTotalCount = dto.totalCount() + count;
            long newApprovedCount = dto.approvedCount();
            long newPendingCount = dto.pendingCount();
            long newCanceledCount = dto.canceledCount();
            long newRejectedCount = dto.rejectedCount();
            long newOngoingCount = dto.ongoingCount();
            long newCompletedCount = dto.completedCount();

            switch (eventStatus) {
                case APPROVED:
                    newApprovedCount += count;
                    break;
                case PENDING:
                    newPendingCount += count;
                    break;
                case CANCELED:
                    newCanceledCount += count;
                    break;
                case REJECTED:
                    newRejectedCount += count;
                    break;
                case ONGOING:
                    newOngoingCount += count;
                    break;
                case COMPLETED:
                    newCompletedCount += count;
                    break;
                default:
                    break;
            }
            summariesByType.put(
                    eventType,
                    new EventTypeSummaryDTO(
                            eventType,
                            newTotalCount,
                            newApprovedCount,
                            newPendingCount,
                            newCanceledCount,
                            newRejectedCount,
                            newOngoingCount,
                            newCompletedCount));
        }

        return summariesByType.values().stream()
                .sorted(Comparator.comparing(EventTypeSummaryDTO::totalCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
