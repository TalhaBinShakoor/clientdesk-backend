package com.clientdesk.projecttask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectTaskUpdateRequest(
        @NotNull
        UUID workRequestId,

        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        ProjectTaskStatus status,

        @Size(max = 200)
        String assignee,

        LocalDate dueDate
) {
}
