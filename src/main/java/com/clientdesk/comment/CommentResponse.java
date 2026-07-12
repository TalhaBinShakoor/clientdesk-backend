package com.clientdesk.comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID workRequestId,
        UUID projectTaskId,
        String workRequestTitle,
        String projectTaskTitle,
        String authorName,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
    static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getWorkRequest() == null ? null : comment.getWorkRequest().getId(),
                comment.getProjectTask() == null ? null : comment.getProjectTask().getId(),
                comment.getWorkRequest() == null ? null : comment.getWorkRequest().getTitle(),
                comment.getProjectTask() == null ? null : comment.getProjectTask().getTitle(),
                comment.getAuthorName(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
