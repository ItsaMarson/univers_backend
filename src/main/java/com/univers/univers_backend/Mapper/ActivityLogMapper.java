/* (C)2025 */
package com.univers.univers_backend.Mapper;

import com.univers.univers_backend.DTO.ActivityLogDTO;
import com.univers.univers_backend.Entity.ActivityLog;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogMapper {

    public ActivityLogDTO toDTO(ActivityLog activityLog) {
        if (activityLog == null) {
            return null;
        }
        return new ActivityLogDTO(
                activityLog.getPublicId(),
                activityLog.getAction(),
                activityLog.getEntityType(),
                activityLog.getEntityId(),
                activityLog.getUserEmail(),
                activityLog.getDetails(),
                activityLog.getIpAddress(),
                activityLog.getCreatedAt());
    }
}
