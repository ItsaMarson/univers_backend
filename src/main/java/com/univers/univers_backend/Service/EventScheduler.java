/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Enum.Status;
import com.univers.univers_backend.Repository.EventRepository;
import java.time.LocalDateTime;
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

    public EventScheduler(
            EventRepository eventRepository, NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 */5 * * * ?") // Runs every 5 minutes
    @Transactional
    public void updateEventsToOngoing() {
        LocalDateTime now = LocalDateTime.now();
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

    @Scheduled(cron = "0 */5 * * * ?") // Runs every 5 minutes
    @Transactional
    public void updateEventsToCompleted() {
        LocalDateTime now = LocalDateTime.now();
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
}
