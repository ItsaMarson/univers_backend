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
import com.univers.univers_backend.Entity.EquipmentReservation;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Mapper.EventMapper;
import com.univers.univers_backend.Repository.EquipmentReservationRepository;
import com.univers.univers_backend.Repository.EventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
            EventMapper eventMapper) { // Added EventMapper
        this.eventRepository = eventRepository;
        this.equipmentReservationRepository = equipmentReservationRepository;
        this.eventMapper = eventMapper;
    }

    public List<TopVenueDTO> getTopVenues(LocalDate startDate, LocalDate endDate, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> results =
                eventRepository.findTopVenuesByEventCount(startInstant, endInstant, pageable);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(result -> new TopVenueDTO((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
    }

    public List<TopEquipmentDTO> getTopEquipment(
            LocalDate startDate, LocalDate endDate, String equipmentTypeName, int limit) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        Pageable pageable = PageRequest.of(0, limit);

        String filterTypeName = StringUtils.hasText(equipmentTypeName) ? equipmentTypeName : null;

        List<Object[]> results =
                equipmentReservationRepository.findTopEquipmentByReservationCount(
                        startInstant, endInstant, filterTypeName, pageable);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(result -> new TopEquipmentDTO((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
    }

    public List<EventCountDTO> getEventsOverview(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        // For endDate, we query up to the start of the next day (exclusive)
        Instant endInstantPlusOne = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> results =
                eventRepository.findEventsOverviewByDate(startInstant, endInstantPlusOne);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(
                        result -> {
                            // The date from FUNCTION('DATE', e.startTime) might be java.sql.Date or
                            // other type
                            // depending on JPA provider and database.
                            // Explicitly convert to LocalDate.
                            LocalDate eventDate;
                            if (result[0] instanceof java.sql.Date) {
                                eventDate = ((java.sql.Date) result[0]).toLocalDate();
                            } else if (result[0]
                                    instanceof
                                    LocalDate) { // Some JPA providers might directly return
                                // LocalDate
                                eventDate = (LocalDate) result[0];
                            } else if (result[0]
                                    != null) { // Fallback for other types, e.g., String, then parse
                                try {
                                    eventDate = LocalDate.parse(result[0].toString());
                                } catch (Exception e) {
                                    // Log error or handle as appropriate, for now, skip this entry
                                    // or use a default
                                    System.err.println(
                                            "Could not parse date from query result: "
                                                    + result[0]
                                                    + " Error: "
                                                    + e.getMessage());
                                    return null; // Or throw, or return a DTO with a placeholder
                                }
                            } else {
                                return null; // Skip if date is null
                            }
                            Long count = (Long) result[1];
                            return new EventCountDTO(eventDate, count);
                        })
                .filter(dto -> dto != null) // Remove any nulls from parsing errors
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

                            long canceledCount =
                                    ((Number) result[1])
                                            .longValue(); // SUM can return various Number types
                            long totalCreatedCount =
                                    ((Number) result[2])
                                            .longValue(); // COUNT returns Long, but being safe

                            double rate = 0.0;
                            if (totalCreatedCount > 0) {
                                rate = ((double) canceledCount / totalCreatedCount) * 100.0;
                            }

                            // Round to 2 decimal places for cleaner output
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
                            if (result[0]
                                    instanceof
                                    Number) { // FUNCTION('HOUR', ...) typically returns an Integer
                                // or BigDecimal
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
                            if (hourOfDay == null)
                                return null; // Skip if hour is null or unparseable

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
                    String description = "Event '" + event.getEventName() + "' was last updated.";
                    if (event.getCreatedAt().equals(event.getUpdatedAt())) {
                        description = "Event '" + event.getEventName() + "' created.";
                    } else {
                        description =
                                "Event '"
                                        + event.getEventName()
                                        + "' status is "
                                        + event.getStatus().toString().toLowerCase()
                                        + ".";
                    }
                    activityItems.add(
                            new RecentActivityItemDTO(
                                    event.getPublicId().toString(),
                                    "Event",
                                    event.getEventName(),
                                    description,
                                    event.getUpdatedAt(),
                                    event.getOrganizer() != null
                                            ? event.getOrganizer().getFirstname()
                                                    + " "
                                                    + event.getOrganizer().getLastname()
                                            : "System",
                                    "/app/events/details/" + event.getPublicId().toString()));
                });

        recentReservations.forEach(
                reservation -> {
                    String description =
                            "Reservation for '"
                                    + reservation.getEquipment().getName()
                                    + "' was last updated.";
                    if (reservation.getCreatedAt().equals(reservation.getUpdatedAt())) {
                        description =
                                "Reservation for '"
                                        + reservation.getEquipment().getName()
                                        + "' created.";
                    } else {
                        description =
                                "Reservation for '"
                                        + reservation.getEquipment().getName()
                                        + "' status is "
                                        + reservation.getStatus().toString().toLowerCase()
                                        + ".";
                    }
                    activityItems.add(
                            new RecentActivityItemDTO(
                                    reservation.getPublicId().toString(),
                                    "Equipment Reservation",
                                    "Reservation for " + reservation.getEquipment().getName(),
                                    description,
                                    reservation.getUpdatedAt(),
                                    reservation.getRequestingUser() != null
                                            ? reservation.getRequestingUser().getFirstname()
                                                    + " "
                                                    + reservation.getRequestingUser().getLastname()
                                            : "System",
                                    reservation.getPublicId().toString()));
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
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> results =
                eventRepository.findEventCountsByEventType(
                        startInstant, endInstantPlusOne, pageable);

        if (results == null) return new ArrayList<>();

        return results.stream()
                .map(
                        result -> {
                            String eventType = (String) result[0];
                            Long count = (Long) result[1];
                            if (eventType == null) return null;
                            return new EventTypeSummaryDTO(eventType, count);
                        })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
