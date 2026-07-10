package com.clientdesk.workrequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkRequestResponse(
        UUID id,
        UUID clientId,
        String clientCompanyName,
        String title,
        String description,
        WorkRequestStatus status,
        WorkRequestPriority priority,
        String requestedBy,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    static WorkRequestResponse from(WorkRequest workRequest) {
        return new WorkRequestResponse(
                workRequest.getId(),
                workRequest.getClient().getId(),
                workRequest.getClient().getCompanyName(),
                workRequest.getTitle(),
                workRequest.getDescription(),
                workRequest.getStatus(),
                workRequest.getPriority(),
                workRequest.getRequestedBy(),
                workRequest.getDueDate(),
                workRequest.getCreatedAt(),
                workRequest.getUpdatedAt()
        );
    }
}
