package com.clientdesk.quote;

import jakarta.validation.constraints.NotNull;

public record QuoteStatusUpdateRequest(
        @NotNull
        QuoteStatus status
) {
}
