package com.clientdesk.ai;

import jakarta.validation.constraints.Pattern;

public record DraftClientReplyRequest(
        @Pattern(regexp = "professional|friendly")
        String tone
) {
}
