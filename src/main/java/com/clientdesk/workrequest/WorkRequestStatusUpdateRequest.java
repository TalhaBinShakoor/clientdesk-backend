package com.clientdesk.workrequest;

import jakarta.validation.constraints.NotNull;

public record WorkRequestStatusUpdateRequest(
        @NotNull
        WorkRequestStatus status
) {
}
