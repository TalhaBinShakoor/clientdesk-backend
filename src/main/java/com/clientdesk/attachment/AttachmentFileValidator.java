package com.clientdesk.attachment;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

@Component
public class AttachmentFileValidator {

    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".pdf", "application/pdf",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".txt", "text/plain",
            ".csv", "text/csv"
    );

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final AttachmentProperties properties;

    public AttachmentFileValidator(AttachmentProperties properties) {
        this.properties = properties;
    }

    ValidatedAttachmentFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }
        if (file.getSize() > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
        }

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionFrom(originalFileName);
        String expectedContentType = CONTENT_TYPE_BY_EXTENSION.get(extension);
        if (expectedContentType == null || !properties.getAllowedContentTypes().contains(expectedContentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "File type is not allowed");
        }

        String declaredContentType = normalizeContentType(file.getContentType());
        if (!expectedContentType.equals(declaredContentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "File extension and content type do not match");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "File could not be read", exception);
        }
        if (content.length > properties.getMaxFileBytes()) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
        }

        validateContent(extension, content);
        return new ValidatedAttachmentFile(originalFileName, extension, expectedContentType, content);
    }

    private void validateContent(String extension, byte[] content) {
        boolean valid = switch (extension) {
            case ".pdf" -> startsWith(content, PDF_SIGNATURE);
            case ".png" -> startsWith(content, PNG_SIGNATURE);
            case ".jpg", ".jpeg" -> startsWith(content, JPEG_SIGNATURE);
            case ".txt", ".csv" -> isSafeUtf8Text(content);
            default -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(BAD_REQUEST, "File content does not match its type");
        }
    }

    private boolean isSafeUtf8Text(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return false;
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeFileName(String fileName) {
        try {
            String sanitized = Path.of(fileName == null ? "attachment" : fileName)
                    .getFileName()
                    .toString()
                    .trim()
                    .replaceAll("[\\r\\n]", "");
            if (sanitized.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "File name is required");
            }
            return sanitized.length() > 255 ? sanitized.substring(sanitized.length() - 255) : sanitized;
        } catch (InvalidPathException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid file name");
        }
    }

    private String extensionFrom(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterIndex = contentType.indexOf(';');
        String mediaType = parameterIndex < 0 ? contentType : contentType.substring(0, parameterIndex);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }
}
