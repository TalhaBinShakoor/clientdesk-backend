package com.clientdesk.attachment;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

class AttachmentFileValidatorTest {

    private final AttachmentProperties properties = properties();
    private final AttachmentFileValidator validator = new AttachmentFileValidator(properties);

    @Test
    void rejectsOversizedFile() {
        properties.setMaxFileBytes(4);

        assertRejected(file("large.txt", MediaType.TEXT_PLAIN_VALUE, "12345".getBytes()), PAYLOAD_TOO_LARGE);
    }

    @Test
    void rejectsDisallowedExtensionAndMismatchedContentType() {
        assertRejected(file("payload.exe", "application/octet-stream", new byte[]{1, 2, 3}), BAD_REQUEST);
        assertRejected(file("document.pdf", MediaType.TEXT_PLAIN_VALUE, "%PDF-1.7".getBytes()), BAD_REQUEST);
    }

    @Test
    void rejectsForgedBinarySignatures() {
        assertRejected(file("image.png", MediaType.IMAGE_PNG_VALUE, "not-a-png".getBytes()), BAD_REQUEST);
        assertRejected(file("image.jpg", MediaType.IMAGE_JPEG_VALUE, "not-a-jpeg".getBytes()), BAD_REQUEST);
        assertRejected(file("document.pdf", MediaType.APPLICATION_PDF_VALUE, "not-a-pdf".getBytes()), BAD_REQUEST);
    }

    @Test
    void rejectsUnsafeTextContent() {
        assertRejected(file("nul.txt", MediaType.TEXT_PLAIN_VALUE, new byte[]{'a', 0, 'b'}), BAD_REQUEST);
        assertRejected(file("invalid.csv", "text/csv", new byte[]{(byte) 0xC3, 0x28}), BAD_REQUEST);
    }

    @Test
    void acceptsAllowedSignaturesAndCanonicalizesTraversalName() {
        ValidatedAttachmentFile pdf = validator.validate(file(
                "../../report.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII)
        ));

        assertEquals("report.pdf", pdf.originalFileName());
        assertEquals(".pdf", pdf.extension());
        assertEquals(MediaType.APPLICATION_PDF_VALUE, pdf.contentType());
    }

    private AttachmentProperties properties() {
        AttachmentProperties result = new AttachmentProperties();
        result.setMaxFileBytes(100);
        return result;
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private void assertRejected(MockMultipartFile file, org.springframework.http.HttpStatus expectedStatus) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(file)
        );
        assertEquals(expectedStatus.value(), exception.getStatusCode().value());
    }
}
