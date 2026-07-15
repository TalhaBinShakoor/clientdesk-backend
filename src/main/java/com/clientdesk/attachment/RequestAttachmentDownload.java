package com.clientdesk.attachment;

import org.springframework.core.io.Resource;

public record RequestAttachmentDownload(
        RequestAttachment attachment,
        Resource resource
) {
}
