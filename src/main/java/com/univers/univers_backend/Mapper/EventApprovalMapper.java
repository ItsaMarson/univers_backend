/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.EventApprovalDTO;
import com.univers.univers_backend.DTO.UserDTO;
import com.univers.univers_backend.Entity.EventApproval;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EventApprovalMapper {

    private final UserMapper userMapper;

    // private final EventMapper eventMapper; // Potentially needed if eventPublicId requires
    // fetching Event entity

    public EventApprovalMapper(@Lazy UserMapper userMapper /*, @Lazy EventMapper eventMapper */) {
        this.userMapper = userMapper;
        // this.eventMapper = eventMapper;
    }

    public EventApprovalDTO toDto(EventApproval approval) {
        if (approval == null) {
            return null;
        }

        UserDTO signedByUserDto = userMapper.toDto(approval.getSignedBy());

        // The DTO has eventPublicId (UUID). The Entity has Event event.
        // We need to get the publicId from the linked Event entity.
        java.util.UUID eventPublicId = null;
        if (approval.getEvent() != null) {
            eventPublicId = approval.getEvent().getPublicId();
        }

        return new EventApprovalDTO(
                approval.getPublicId(),
                eventPublicId,
                signedByUserDto,
                null, // userRole - DTO has this, but where does it come from? Maybe from
                // signedByUserDto.getRole()?
                approval.getRemarks(),
                approval.getStatus() != null ? approval.getStatus().name() : null,
                approval.getDateSigned());
    }
}
