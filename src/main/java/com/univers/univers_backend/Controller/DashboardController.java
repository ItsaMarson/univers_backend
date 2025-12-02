/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.EventDTO;
import com.univers.univers_backend.DTO.dashboard.CancellationRateDTO;
import com.univers.univers_backend.DTO.dashboard.EventCountDTO;
import com.univers.univers_backend.DTO.dashboard.EventTypeSummaryDTO;
import com.univers.univers_backend.DTO.dashboard.PeakHourDTO;
import com.univers.univers_backend.DTO.dashboard.RecentActivityItemDTO;
import com.univers.univers_backend.DTO.dashboard.TopDepartmentDTO;
import com.univers.univers_backend.DTO.dashboard.TopEquipmentDTO;
import com.univers.univers_backend.DTO.dashboard.TopVenueDTO;
import com.univers.univers_backend.DTO.dashboard.UserActivityDTO;
import com.univers.univers_backend.DTO.dashboard.UserReservationActivityDTO;
import com.univers.univers_backend.Service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard Controller", description = "Endpoints for fetching dashboard analytics data")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/top-venues")
    @Operation(
            summary = "Get top venues by event count",
            description =
                    "Fetches a list of top venues, including their total event counts and a"
                        + " breakdown of these counts by status (e.g., Approved, Pending,"
                        + " Canceled), within a specified date range. Venues are ordered by their"
                        + " total event count in descending order.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved top venues",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = TopVenueDTO.class,
                                                        type = "array"))),
                @ApiResponse(responseCode = "400", description = "Invalid date range provided")
            })
    public ResponseEntity<List<TopVenueDTO>> getTopVenues(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(description = "Number of top venues to return", example = "5")
                    @RequestParam(defaultValue = "5")
                    int limit) {
        List<TopVenueDTO> topVenues = dashboardService.getTopVenues(startDate, endDate, limit);
        return ResponseEntity.ok(topVenues);
    }

    @GetMapping("/top-equipment")
    @Operation(
            summary = "Get top equipment by reservation count",
            description =
                    "Fetches a list of top equipment, including their total reservation counts and"
                        + " a breakdown of these counts by status (e.g., Pending, Approved,"
                        + " Canceled, Ongoing, Completed), within a specified date range. Equipment"
                        + " is ordered by total reservation count in descending order and can be"
                        + " optionally filtered by name/type.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved top equipment",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = TopEquipmentDTO.class,
                                                        type = "array"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid date range or parameters provided")
            })
    public ResponseEntity<List<TopEquipmentDTO>> getTopEquipment(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(
                            description =
                                    "Optional filter for equipment name/type (case-insensitive,"
                                            + " partial match)",
                            example = "Projector")
                    @RequestParam(required = false)
                    String equipmentTypeFilter,
            @Parameter(description = "Number of top equipment to return", example = "5")
                    @RequestParam(defaultValue = "5")
                    int limit) {
        List<TopEquipmentDTO> topEquipment =
                dashboardService.getTopEquipment(startDate, endDate, equipmentTypeFilter, limit);
        return ResponseEntity.ok(topEquipment);
    }

    @GetMapping("/events-overview")
    @Operation(
            summary = "Get overview of event counts per day",
            description =
                    "Fetches a list of dates and corresponding event counts within a specified date"
                            + " range.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved event overview data",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = EventCountDTO.class,
                                                        type = "array"))),
                @ApiResponse(responseCode = "400", description = "Invalid date range provided")
            })
    public ResponseEntity<List<EventCountDTO>> getEventsOverview(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        List<EventCountDTO> eventsOverview = dashboardService.getEventsOverview(startDate, endDate);
        return ResponseEntity.ok(eventsOverview);
    }

    @GetMapping("/cancellation-rate")
    @Operation(
            summary = "Get daily event cancellation rates",
            description =
                    "Fetches daily cancellation rates for events created within the specified date"
                            + " range.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved cancellation rate data",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = CancellationRateDTO.class,
                                                        type = "array"))),
                @ApiResponse(responseCode = "400", description = "Invalid date range provided")
            })
    public ResponseEntity<List<CancellationRateDTO>> getCancellationRates(
            @Parameter(
                            description =
                                    "Start date for the filter (YYYY-MM-DD, based on event creation"
                                            + " date)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description =
                                    "End date for the filter (YYYY-MM-DD, based on event creation"
                                            + " date)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        List<CancellationRateDTO> cancellationRates =
                dashboardService.getCancellationRates(startDate, endDate);
        return ResponseEntity.ok(cancellationRates);
    }

    @GetMapping("/peak-hours")
    @Operation(
            summary = "Get peak reservation hours for events",
            description =
                    "Fetches event counts grouped by the hour of their start time within a"
                            + " specified date range.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved peak hour data",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = PeakHourDTO.class,
                                                        type = "array"))),
                @ApiResponse(responseCode = "400", description = "Invalid date range provided")
            })
    public ResponseEntity<List<PeakHourDTO>> getPeakReservationHours(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        List<PeakHourDTO> peakHours = dashboardService.getPeakReservationHours(startDate, endDate);
        return ResponseEntity.ok(peakHours);
    }

    @GetMapping("/user-activity")
    @Operation(
            summary = "Get user event activity",
            description =
                    "Fetches a list of users ordered by the number of events they organized within"
                            + " a specified date range.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved user activity data",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = UserActivityDTO.class,
                                                        type = "array"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid date range or parameters provided")
            })
    public ResponseEntity<List<UserActivityDTO>> getUserActivity(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(description = "Number of top active users to return", example = "10")
                    @RequestParam(defaultValue = "10")
                    int limit) {
        List<UserActivityDTO> userActivity =
                dashboardService.getUserActivity(startDate, endDate, limit);
        return ResponseEntity.ok(userActivity);
    }

    @GetMapping("/recent-activity")
    @Operation(
            summary = "Get recent activity feed",
            description =
                    "Fetches a list of recent activities like event/reservation creations and"
                            + " updates.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved recent activity",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                RecentActivityItemDTO.class,
                                                        type = "array")))
            })
    public ResponseEntity<List<RecentActivityItemDTO>> getRecentActivity(
            @Parameter(description = "Number of recent activities to return", example = "10")
                    @RequestParam(defaultValue = "10")
                    int limit) {
        List<RecentActivityItemDTO> recentActivity = dashboardService.getRecentActivity(limit);
        return ResponseEntity.ok(recentActivity);
    }

    @GetMapping("/upcoming-approved-events")
    @Operation(
            summary = "Get upcoming approved events",
            description = "Fetches a list of approved events that are starting soon.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved upcoming approved events",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = EventDTO.class,
                                                        type = "array")))
            })
    public ResponseEntity<List<EventDTO>> getUpcomingApprovedEvents(
            @Parameter(description = "Number of upcoming events to return", example = "5")
                    @RequestParam(defaultValue = "5")
                    int limit) {
        List<EventDTO> upcomingEvents = dashboardService.getUpcomingApprovedEvents(limit);
        return ResponseEntity.ok(upcomingEvents);
    }

    @GetMapping("/upcoming-approved-events-count-next-days")
    @Operation(
            summary = "Get count of upcoming approved events for next N days",
            description =
                    "Fetches the count of approved events starting within the specified number of"
                            + " days from now.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved the count",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Long.class)))
            })
    public ResponseEntity<Long> getUpcomingApprovedEventsCountNextDays(
            @Parameter(
                            description =
                                    "Number of days from now to look ahead for upcoming events",
                            example = "30")
                    @RequestParam(defaultValue = "30")
                    int days) {
        long count = dashboardService.getUpcomingApprovedEventCountForNextDays(days);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/event-types-summary")
    @Operation(
            summary = "Get event types summary",
            description =
                    "Fetches a summary of event types, including total counts and a breakdown by"
                            + " status (e.g., Approved, Pending, Canceled), within a specified date"
                            + " range. Results are ordered by total count in descending order.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved event types summary",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = EventTypeSummaryDTO.class,
                                                        type = "array")))
            })
    public ResponseEntity<List<EventTypeSummaryDTO>> getEventTypesSummary(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(description = "Number of top event types to return", example = "5")
                    @RequestParam(defaultValue = "5")
                    int limit) {
        List<EventTypeSummaryDTO> eventTypesSummary =
                dashboardService.getEventCountsByEventType(startDate, endDate, limit);
        return ResponseEntity.ok(eventTypesSummary);
    }

    @GetMapping("/user-reservation-activity")
    @Operation(
            summary = "Get user reservation activity by count",
            description =
                    "Fetches a list of users ordered by their total number of equipment"
                        + " reservations, including a breakdown of reservation counts by status"
                        + " (e.g., Pending, Approved, Canceled, Ongoing, Completed), within a"
                        + " specified date range. Can be optionally filtered by user's first name,"
                        + " last name, or full name.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved user reservation activity",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                UserReservationActivityDTO.class,
                                                        type = "array"))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid date range or parameters provided")
            })
    public ResponseEntity<List<UserReservationActivityDTO>> getUserReservationActivity(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(
                            description =
                                    "Optional filter for user's first name, last name, or full name"
                                            + " (case-insensitive, partial match)",
                            example = "John Doe")
                    @RequestParam(required = false)
                    String userFilter,
            @Parameter(
                            description = "Number of top users to return by reservation count",
                            example = "10")
                    @RequestParam(defaultValue = "10")
                    int limit) {
        List<UserReservationActivityDTO> userActivity =
                dashboardService.getUserReservationActivity(startDate, endDate, userFilter, limit);
        return ResponseEntity.ok(userActivity);
    }

    @GetMapping("/top-departments")
    @Operation(
            summary = "Get top departments by reservation rate",
            description =
                    "Fetches a list of top departments ranked by their reservation rate (percentage"
                        + " of approved, ongoing, and completed events out of total events) within"
                        + " a specified date range. The reservation rate is calculated as (approved"
                        + " + ongoing + completed) / total * 100. Departments are ordered by"
                        + " reservation rate in descending order.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved top departments",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = TopDepartmentDTO.class,
                                                        type = "array"))),
                @ApiResponse(responseCode = "400", description = "Invalid date range provided")
            })
    public ResponseEntity<List<TopDepartmentDTO>> getTopDepartments(
            @Parameter(
                            description = "Start date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-01-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @Parameter(
                            description = "End date for the filter (YYYY-MM-DD)",
                            required = true,
                            example = "2023-12-31")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @Parameter(description = "Number of top departments to return", example = "5")
                    @RequestParam(defaultValue = "5")
                    int limit) {
        List<TopDepartmentDTO> topDepartments =
                dashboardService.getTopDepartments(startDate, endDate, limit);
        return ResponseEntity.ok(topDepartments);
    }
}
