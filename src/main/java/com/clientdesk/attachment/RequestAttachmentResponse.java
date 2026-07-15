package com.clientdesk.attachment;

import java.time.Instant;
import java.util.UUID;

public record RequestAttachmentResponse(
        UUID id,
        UUID workRequestId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant createdAt
) {

    public static RequestAttachmentResponse from(RequestAttachment attachment) {
        return new RequestAttachmentResponse(
                attachment.getId(),
                attachment.getWorkRequest().getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt()
        );
    }
}
