/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EventScheduler.class);

    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final EquipmentReservationService equipmentReservationService;

    public EventScheduler(
            EventRepository eventRepository,
            NotificationService notificationService,
            EquipmentReservationService equipmentReservationService) {
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
        this.equipmentReservationService = equipmentReservationService;
    }

    @Scheduled(cron = "0 */1 * * * ?") // Runs every 1 minute
    @Transactional
    public void updateEventsToOngoing() {
        Instant now = Instant.now();
        logger.info("Scheduler: Checking for events to set to ONGOING at {}", now);
        List<Event> eventsToStart =
                eventRepository.findByStatusAndStartTimeBeforeAndEndTimeAfter(
                        Status.APPROVED, now, now);

        if (eventsToStart.isEmpty()) {
            logger.info("Scheduler: No approved events ready to start.");
            return;
        }

        for (Event event : eventsToStart) {
            event.setStatus(Status.ONGOING);
            eventRepository.save(event);
            logger.info(
                    "Event {} (Public ID: {}) status updated to ONGOING.",
                    event.getEventName(),
                    event.getPublicId());
            // Optional: Notify organizer
            if (event.getOrganizer() != null) {
                notificationService.createNotification(
                        event.getOrganizer(),
                        "Your event '" + event.getEventName() + "' is now ONGOING.",
                        event.getPublicId(), // relatedEntityId (event itself)
                        event.getPublicId(), // entityIdForNavigation (navigate to event details)
                        "EVENT_ONGOING" // notificationType
                        );
            }
        }
        logger.info(
                "Scheduler: Finished checking for events to set to ONGOING. {} events updated.",
                eventsToStart.size());
    }

    @Scheduled(cron = "0 */1 * * * ?") // Runs every 1 minute
    @Transactional
    public void updateEventsToCompleted() {
        Instant now = Instant.now();
        logger.info("Scheduler: Checking for events to set to COMPLETED at {}", now);
        List<Event> eventsToComplete =
                eventRepository.findByStatusAndEndTimeBefore(Status.ONGOING, now);

        if (eventsToComplete.isEmpty()) {
            logger.info("Scheduler: No ongoing events ready to complete.");
            return;
        }

        for (Event event : eventsToComplete) {
            event.setStatus(Status.COMPLETED);
            eventRepository.save(event);
            logger.info(
                    "Event {} (Public ID: {}) status updated to COMPLETED.",
                    event.getEventName(),
                    event.getPublicId());

            // Note: Equipment restoration is now handled by time-based scheduler
            // that runs every 5 minutes and checks reservation end times directly

            // Optional: Notify organizer
            if (event.getOrganizer() != null) {
                notificationService.createNotification(
                        event.getOrganizer(),
                        "Your event '" + event.getEventName() + "' has been COMPLETED.",
                        event.getPublicId(),
                        event.getPublicId(),
                        "EVENT_COMPLETED");
            }
        }
        logger.info(
                "Scheduler: Finished checking for events to set to COMPLETED. {} events updated.",
                eventsToComplete.size());
    }

    @Scheduled(cron = "0 */1 * * * ?") // Runs every 1 minutes
    @Transactional
    public void restoreEquipmentForExpiredReservations() {
        Instant now = Instant.now();
        logger.info("Scheduler: Checking for equipment reservations to restore at {}", now);

        try {
            equipmentReservationService.restoreEquipmentForExpiredReservations(now);
        } catch (Exception e) {
            logger.error(
                    "Error restoring equipment for expired reservations: {}", e.getMessage(), e);
        }

        logger.info("Scheduler: Finished checking for equipment reservations to restore.");
    }

    @Scheduled(cron = "0 */1 * * * ?") // Runs every 1 minutes
    @Transactional
    public void cancelExpiredPendingEvents() {
        Instant now = Instant.now();
        logger.info("Scheduler: Checking for pending events past end time to cancel at {}", now);
        List<Event> expiredPendingEvents =
                eventRepository.findByStatusAndEndTimeBefore(Status.PENDING, now);

        if (expiredPendingEvents.isEmpty()) {
            logger.info("Scheduler: No pending events past end time found.");
            return;
        }

        for (Event event : expiredPendingEvents) {
            event.setStatus(Status.CANCELED);
            eventRepository.save(event);
            logger.info(
                    "Event {} (Public ID: {}) status updated from PENDING to CANCELED (past end"
                            + " time).",
                    event.getEventName(),
                    event.getPublicId());

            // Notify organizer
            if (event.getOrganizer() != null) {
                notificationService.createNotification(
                        event.getOrganizer(),
                        "Your event '"
                                + event.getEventName()
                                + "' has been automatically canceled because it passed the end time"
                                + " while still pending approval.",
                        event.getPublicId(),
                        event.getPublicId(),
                        "EVENT_AUTO_CANCELED");
            }
        }
        logger.info(
                "Scheduler: Finished checking for expired pending events. {} events canceled.",
                expiredPendingEvents.size());
    }
}
