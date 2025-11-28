/* (C)2025 */
package com.univers.univers_backend.Controller;

import com.univers.univers_backend.DTO.ActivityLogDTO;
import com.univers.univers_backend.Service.ActivityLogService;
import com.univers.univers_backend.config.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/activity-logs")
@Tag(name = "Activity Logs", description = "Activity logging and audit trail APIs for Super Admin")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Operation(
            summary = "Get all activity logs",
            description = "Retrieves paginated activity logs for all users and actions")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Activity logs retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ActivityLogDTO>>> getAllLogs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogDTO> logs = activityLogService.getAllLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", logs));
    }

    @Operation(
            summary = "Get logs by action",
            description = "Retrieves paginated activity logs filtered by action type")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Activity logs retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/by-action")
    public ResponseEntity<ApiResponse<Page<ActivityLogDTO>>> getLogsByAction(
            @Parameter(description = "Action to filter by") @RequestParam String action,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogDTO> logs = activityLogService.getLogsByAction(action, pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", logs));
    }

    @Operation(
            summary = "Get logs by entity type",
            description = "Retrieves paginated activity logs filtered by entity type")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Activity logs retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/by-entity-type")
    public ResponseEntity<ApiResponse<Page<ActivityLogDTO>>> getLogsByEntityType(
            @Parameter(description = "Entity type to filter by") @RequestParam String entityType,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogDTO> logs = activityLogService.getLogsByEntityType(entityType, pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", logs));
    }

    @Operation(
            summary = "Get logs by date range",
            description = "Retrieves paginated activity logs filtered by date range")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "Activity logs retrieved successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "Invalid date format"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/by-date-range")
    public ResponseEntity<ApiResponse<Page<ActivityLogDTO>>> getLogsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLogDTO> logs =
                activityLogService.getLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", logs));
    }

    @Operation(
            summary = "Export all logs as CSV",
            description = "Exports all activity logs as a CSV file")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "CSV file generated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportLogsAsCSV(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endDate) {
        try {
            byte[] csvData;
            String filename;

            if (startDate != null && endDate != null) {
                csvData = activityLogService.exportLogsAsCSV(startDate, endDate);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                filename =
                        String.format(
                                "activity_logs_%s_to_%s.csv",
                                startDate.format(formatter), endDate.format(formatter));
            } else {
                csvData = activityLogService.exportLogsAsCSV();
                filename =
                        String.format(
                                "activity_logs_%s.csv",
                                LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Export all logs as JSON",
            description = "Exports all activity logs as a JSON file")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "JSON file generated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "Access denied - Super Admin role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "Internal server error")
            })
    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportLogsAsJSON(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime endDate) {
        try {
            byte[] jsonData;
            String filename;

            if (startDate != null && endDate != null) {
                jsonData = activityLogService.exportLogsAsJSON(startDate, endDate);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                filename =
                        String.format(
                                "activity_logs_%s_to_%s.json",
                                startDate.format(formatter), endDate.format(formatter));
            } else {
                jsonData = activityLogService.exportLogsAsJSON();
                filename =
                        String.format(
                                "activity_logs_%s.json",
                                LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(jsonData, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
