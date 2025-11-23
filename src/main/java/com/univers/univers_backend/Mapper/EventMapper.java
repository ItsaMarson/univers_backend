/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Entity.Event;
import com.univers.univers_backend.Entity.EventPersonnel;
import com.univers.univers_backend.Service.FileStorageService;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final UserMapper userMapper;
    private final VenueMapper venueMapper;
    private final DepartmentMapper departmentMapper;
    private final EventApprovalMapper eventApprovalMapper;
    private final FileStorageService fileStorageService;
    private final String approvedLettersBucketName;
    private final String eventImagesBucketName;

    public EventMapper(
            @Lazy UserMapper userMapper,
            @Lazy VenueMapper venueMapper,
            @Lazy DepartmentMapper departmentMapper,
            @Lazy EventApprovalMapper eventApprovalMapper,
            FileStorageService fileStorageService,
            @Value("${minio.bucket.approved_letters}") String approvedLettersBucketName,
            @Value("${minio.bucket.event_images}") String eventImagesBucketName) {
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
        this.departmentMapper = departmentMapper;
        this.eventApprovalMapper = eventApprovalMapper;
        this.fileStorageService = fileStorageService;
        this.approvedLettersBucketName = approvedLettersBucketName;
        this.eventImagesBucketName = eventImagesBucketName;
    }

    public EventDTO toDto(Event event) {
        if (event == null) {
            return null;
        }

        UserDTO organizerDto = userMapper.toDto(event.getOrganizer());
        VenueDTO venueDto = venueMapper.toDto(event.getEventVenue());
        DepartmentDTO departmentDto = departmentMapper.toDto(event.getDepartment());

        String approvedLetterUrl = null;
        if (event.getApprovedLetterPath() != null && !event.getApprovedLetterPath().isBlank()) {
            try {
                approvedLetterUrl =
                        fileStorageService.getFileUrl(
                                event.getApprovedLetterPath(), approvedLettersBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating approved letter URL for event "
                                + event.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        String imageUrl = null;
        if (event.getImagePath() != null && !event.getImagePath().isBlank()) {
            try {
                imageUrl =
                        fileStorageService.getFileUrl(event.getImagePath(), eventImagesBucketName);
            } catch (Exception e) {
                System.err.println(
                        "Error generating image URL for event "
                                + event.getPublicId()
                                + ": "
                                + e.getMessage());
            }
        }

        List<EventApprovalDTO> approvalDtos = Collections.emptyList();
        if (event.getApprovals() != null) {
            approvalDtos =
                    event.getApprovals().stream()
                            .map(eventApprovalMapper::toDto)
                            .collect(Collectors.toList());
        }

        List<EventPersonnelDTO> personnelDtos = Collections.emptyList();
        if (event.getAssignedPersonnel() != null) {
            personnelDtos =
                    event.getAssignedPersonnel().stream()
                            .map(this::toPersonnelDto)
                            .collect(Collectors.toList());
        }

        return new EventDTO(
                event.getPublicId(),
                event.getEventName(),
                event.getEventType(),
                organizerDto,
                venueDto,
                departmentDto,
                event.getStartTime(),
                event.getEndTime(),
                event.getStatus() != null ? event.getStatus().name() : null,
                approvedLetterUrl,
                imageUrl,
                approvalDtos,
                personnelDtos,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    // public Event toEntity(EventDTO dto) { ... } // If needed later

    public EventPersonnelDTO toPersonnelDto(EventPersonnel newPersonnel) {

        return new EventPersonnelDTO(
                newPersonnel.getPublicId(), newPersonnel.getAssignedPersonnel(), newPersonnel.getPhoneNumber(), newPersonnel.getTask());
    }
}
