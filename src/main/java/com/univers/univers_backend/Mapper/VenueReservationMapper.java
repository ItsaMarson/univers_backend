/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Entity.VenueReservation;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class VenueReservationMapper {

    private final EventMapper eventMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final VenueMapper venueMapper;
    private final VenueApprovalMapper venueApprovalMapper;

    public VenueReservationMapper(
            @Lazy EventMapper eventMapper,
            @Lazy UserMapper userMapper,
            @Lazy DepartmentMapper departmentMapper,
            @Lazy VenueMapper venueMapper,
            @Lazy VenueApprovalMapper venueApprovalMapper) {
        this.eventMapper = eventMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.venueMapper = venueMapper;
        this.venueApprovalMapper = venueApprovalMapper;
    }

    public VenueReservationDTO toDto(VenueReservation reservation) {
        if (reservation == null) {
            return null;
        }

        EventDTO eventDto = eventMapper.toDto(reservation.getEvent());
        UserDTO requestingUserDto = userMapper.toDto(reservation.getRequestingUser());
        DepartmentDTO departmentDto = departmentMapper.toDto(reservation.getDepartment());
        VenueDTO venueDto = venueMapper.toDto(reservation.getVenue());

        java.util.List<VenueApprovalDTO> approvalDtos = Collections.emptyList();
        if (reservation.getApprovals() != null) {
            approvalDtos =
                    reservation.getApprovals().stream()
                            .map(venueApprovalMapper::toDto)
                            .collect(Collectors.toList());
        }

        return new VenueReservationDTO(
                reservation.getPublicId(),
                eventDto,
                requestingUserDto,
                departmentDto,
                venueDto,
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus() != null ? reservation.getStatus().name() : null,
                approvalDtos,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
