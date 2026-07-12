package com.clientdesk.activity;

import java.time.Instant;
import java.util.UUID;

public record ActivityEventResponse(
        UUID id,
        UUID workRequestId,
        UUID projectTaskId,
        UUID commentId,
        ActivityEventType eventType,
        String actorName,
        String summary,
        Instant createdAt
) {
    static ActivityEventResponse from(ActivityEvent activityEvent) {
        return new ActivityEventResponse(
                activityEvent.getId(),
                activityEvent.getWorkRequest().getId(),
                activityEvent.getProjectTask() == null ? null : activityEvent.getProjectTask().getId(),
                activityEvent.getComment() == null ? null : activityEvent.getComment().getId(),
                activityEvent.getEventType(),
                activityEvent.getActorName(),
                activityEvent.getSummary(),
                activityEvent.getCreatedAt()
        );
    }
}
