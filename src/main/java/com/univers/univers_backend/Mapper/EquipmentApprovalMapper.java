/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.EquipmentApprovalDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.EquipmentApproval;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EquipmentApprovalMapper {

    private final UserMapper userMapper;

    // Potentially EquipmentReservationMapper if more details from reservation are needed

    public EquipmentApprovalMapper(@Lazy UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public EquipmentApprovalDTO toDto(EquipmentApproval approval) {
        if (approval == null) {
            return null;
        }

        UserDTO signedByUserDto = userMapper.toDto(approval.getSignedBy());

        UUID reservationPublicId = null;
        if (approval.getEquipmentReservation() != null) {
            reservationPublicId = approval.getEquipmentReservation().getPublicId();
        }

        // The DTO has a 'userRole' field. This might come from signedByUserDto.getRole()
        // or another source. Placeholder for now.
        Set<String> userRole = signedByUserDto != null ? signedByUserDto.roles() : null;

        return new EquipmentApprovalDTO(
                approval.getPublicId(),
                reservationPublicId,
                signedByUserDto,
                userRole,
                approval.getRemarks(),
                approval.getStatus() != null ? approval.getStatus().name() : null,
                approval.getDateSigned());
    }
}
