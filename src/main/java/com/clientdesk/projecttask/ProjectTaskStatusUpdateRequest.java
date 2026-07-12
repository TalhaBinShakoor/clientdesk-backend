package com.clientdesk.projecttask;

import jakarta.validation.constraints.NotNull;

public record ProjectTaskStatusUpdateRequest(
        @NotNull
        ProjectTaskStatus status
) {
}
