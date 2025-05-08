/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.DTO.VenueApprovalDTO;
import com.univers.univers_backend.Entity.VenueApproval;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class VenueApprovalMapper {

    private final UserMapper userMapper;

    // Potentially VenueReservationMapper if more details from reservation are needed

    public VenueApprovalMapper(@Lazy UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public VenueApprovalDTO toDto(VenueApproval approval) {
        if (approval == null) {
            return null;
        }

        UserDTO signedByUserDto = userMapper.toDto(approval.getSignedBy());

        UUID reservationPublicId = null;
        if (approval.getVenueReservation() != null) {
            reservationPublicId = approval.getVenueReservation().getPublicId();
        }

        // The DTO has a 'userRole' field. This might come from signedByUserDto.getRole()
        // or another source. Placeholder for now.
        String userRole = signedByUserDto != null ? signedByUserDto.role() : null;

        return new VenueApprovalDTO(
                approval.getPublicId(),
                reservationPublicId,
                signedByUserDto,
                userRole,
                approval.getRemarks(),
                approval.getStatus() != null ? approval.getStatus().name() : null,
                approval.getDateSigned());
    }
}
