package com.clientdesk.workrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record WorkRequestCreateRequest(
        @NotNull
        UUID clientId,

        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        WorkRequestStatus status,

        WorkRequestPriority priority,

        @Size(max = 200)
        String requestedBy,

        LocalDate dueDate
) {
}
