package com.clientdesk.attachment;

record ValidatedAttachmentFile(
        String originalFileName,
        String extension,
        String contentType,
        byte[] content
) {
}
