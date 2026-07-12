package com.clientdesk.projecttask;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectTaskResponse(
        UUID id,
        UUID workRequestId,
        String workRequestTitle,
        String clientCompanyName,
        String title,
        String description,
        ProjectTaskStatus status,
        String assignee,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    static ProjectTaskResponse from(ProjectTask projectTask) {
        return new ProjectTaskResponse(
                projectTask.getId(),
                projectTask.getWorkRequest().getId(),
                projectTask.getWorkRequest().getTitle(),
                projectTask.getWorkRequest().getClient().getCompanyName(),
                projectTask.getTitle(),
                projectTask.getDescription(),
                projectTask.getStatus(),
                projectTask.getAssignee(),
                projectTask.getDueDate(),
                projectTask.getCreatedAt(),
                projectTask.getUpdatedAt()
        );
    }
}
