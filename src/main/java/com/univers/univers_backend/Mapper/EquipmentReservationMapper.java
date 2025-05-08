/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.*;
import com.univers.univers_backend.Entity.EquipmentReservation;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EquipmentReservationMapper {

    private final EventMapper eventMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentApprovalMapper equipmentApprovalMapper;

    public EquipmentReservationMapper(
            @Lazy EventMapper eventMapper,
            @Lazy UserMapper userMapper,
            @Lazy DepartmentMapper departmentMapper,
            @Lazy EquipmentMapper equipmentMapper,
            @Lazy EquipmentApprovalMapper equipmentApprovalMapper) {
        this.eventMapper = eventMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.equipmentMapper = equipmentMapper;
        this.equipmentApprovalMapper = equipmentApprovalMapper;
    }

    public EquipmentReservationDTO toDto(EquipmentReservation reservation) {
        if (reservation == null) {
            return null;
        }

        EventDTO eventDto = eventMapper.toDto(reservation.getEvent());
        UserDTO requestingUserDto = userMapper.toDto(reservation.getRequestingUser());
        DepartmentDTO departmentDto = departmentMapper.toDto(reservation.getDepartment());
        EquipmentDTO equipmentDto = equipmentMapper.toDto(reservation.getEquipment());

        java.util.List<EquipmentApprovalDTO> approvalDtos = Collections.emptyList();
        if (reservation.getApprovals() != null) {
            approvalDtos =
                    reservation.getApprovals().stream()
                            .map(equipmentApprovalMapper::toDto)
                            .collect(Collectors.toList());
        }

        return new EquipmentReservationDTO(
                reservation.getPublicId(),
                eventDto,
                requestingUserDto,
                departmentDto,
                equipmentDto,
                reservation.getQuantity(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus() != null ? reservation.getStatus().name() : null,
                approvalDtos,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
