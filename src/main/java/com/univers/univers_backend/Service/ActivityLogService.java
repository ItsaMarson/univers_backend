/* (C)2025 */
package com.univers.univers_backend.Service;

import com.univers.univers_backend.DTO.ActivityLogDTO;
import com.univers.univers_backend.Entity.ActivityLog;
import com.univers.univers_backend.Entity.User;
import com.univers.univers_backend.Mapper.ActivityLogMapper;
import com.univers.univers_backend.Repository.ActivityLogRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;

    public ActivityLogService(
            ActivityLogRepository activityLogRepository, ActivityLogMapper activityLogMapper) {
        this.activityLogRepository = activityLogRepository;
        this.activityLogMapper = activityLogMapper;
    }

    @Transactional
    public void logActivity(
            String action,
            String entityType,
            UUID entityId,
            User user,
            String details,
            String ipAddress) {
        ActivityLog log = new ActivityLog(action, entityType, entityId, user, details, ipAddress);
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getAllLogs(Pageable pageable) {
        Page<ActivityLog> logs = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return logs.map(activityLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsByUser(User user, Pageable pageable) {
        Page<ActivityLog> logs =
                activityLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return logs.map(activityLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsByAction(String action, Pageable pageable) {
        Page<ActivityLog> logs =
                activityLogRepository.findByActionContainingIgnoreCaseOrderByCreatedAtDesc(
                        action, pageable);
        return logs.map(activityLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsByEntityType(String entityType, Pageable pageable) {
        Page<ActivityLog> logs =
                activityLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        return logs.map(activityLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<ActivityLog> logs =
                activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate, endDate, pageable);
        return logs.map(activityLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsAsCSV() throws IOException {
        List<ActivityLog> logs = activityLogRepository.findAllByOrderByCreatedAtDesc();
        return generateCSV(logs);
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsAsCSV(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        List<ActivityLog> logs =
                activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate, endDate);
        return generateCSV(logs);
    }

    private byte[] generateCSV(List<ActivityLog> logs) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer =
                new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            // Write CSV header
            writer.println("Timestamp,Action,Entity Type,Entity ID,User Email,IP Address,Details");

            // Write data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ActivityLog log : logs) {
                writer.println(
                        String.format(
                                "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                                log.getCreatedAt().format(formatter),
                                escapeCSV(log.getAction()),
                                escapeCSV(log.getEntityType()),
                                log.getEntityId() != null ? log.getEntityId().toString() : "",
                                escapeCSV(log.getUserEmail()),
                                escapeCSV(log.getIpAddress()),
                                escapeCSV(log.getDetails())));
            }
        }
        return outputStream.toByteArray();
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsAsJSON() throws IOException {
        List<ActivityLog> logs = activityLogRepository.findAllByOrderByCreatedAtDesc();
        return generateJSON(logs);
    }

    @Transactional(readOnly = true)
    public byte[] exportLogsAsJSON(LocalDateTime startDate, LocalDateTime endDate)
            throws IOException {
        List<ActivityLog> logs =
                activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        startDate, endDate);
        return generateJSON(logs);
    }

    private byte[] generateJSON(List<ActivityLog> logs) {
        List<ActivityLogDTO> dtos =
                logs.stream().map(activityLogMapper::toDTO).collect(Collectors.toList());

        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < dtos.size(); i++) {
            ActivityLogDTO dto = dtos.get(i);
            json.append("  {\n");
            json.append(String.format("    \"publicId\": \"%s\",\n", dto.publicId()));
            json.append(String.format("    \"action\": \"%s\",\n", escapeJSON(dto.action())));
            json.append(
                    String.format("    \"entityType\": \"%s\",\n", escapeJSON(dto.entityType())));
            json.append(
                    String.format(
                            "    \"entityId\": \"%s\",\n",
                            dto.entityId() != null ? dto.entityId().toString() : ""));
            json.append(String.format("    \"userEmail\": \"%s\",\n", escapeJSON(dto.userEmail())));
            json.append(String.format("    \"ipAddress\": \"%s\",\n", escapeJSON(dto.ipAddress())));
            json.append(String.format("    \"details\": \"%s\",\n", escapeJSON(dto.details())));
            json.append(String.format("    \"createdAt\": \"%s\"\n", dto.createdAt()));
            json.append("  }");
            if (i < dtos.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]");

        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeJSON(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
