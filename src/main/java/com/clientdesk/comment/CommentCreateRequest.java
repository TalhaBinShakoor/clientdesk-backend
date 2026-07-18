package com.clientdesk.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommentCreateRequest(
        UUID workRequestId,
        UUID projectTaskId,
        @Size(max = 200) String authorName,
        @NotBlank String body
) {
}
