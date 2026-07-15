package com.clientdesk.attachment;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class RequestAttachmentService {

    private static final Path UPLOAD_ROOT = Paths.get("uploads", "request-attachments")
            .toAbsolutePath()
            .normalize();

    private final RequestAttachmentRepository requestAttachmentRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ActivityEventService activityEventService;

    public RequestAttachmentService(
            RequestAttachmentRepository requestAttachmentRepository,
            WorkRequestRepository workRequestRepository,
            ActivityEventService activityEventService
    ) {
        this.requestAttachmentRepository = requestAttachmentRepository;
        this.workRequestRepository = workRequestRepository;
        this.activityEventService = activityEventService;
    }

    @Transactional(readOnly = true)
    public List<RequestAttachmentResponse> findAll(UUID workRequestId) {
        findWorkRequest(workRequestId);

        return requestAttachmentRepository.findByWorkRequest_IdOrderByCreatedAtDesc(workRequestId)
                .stream()
                .map(RequestAttachmentResponse::from)
                .toList();
    }

    public RequestAttachmentResponse upload(UUID workRequestId, MultipartFile file, String uploadedBy) {
        WorkRequest workRequest = findWorkRequest(workRequestId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is required");
        }

        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extensionFrom(originalFileName);
        Path destination = UPLOAD_ROOT.resolve(storedFileName).normalize();

        if (!destination.startsWith(UPLOAD_ROOT)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(UPLOAD_ROOT);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "File could not be stored", exception);
        }

        RequestAttachment attachment = new RequestAttachment(
                workRequest,
                originalFileName,
                storedFileName,
                normalizeContentType(file.getContentType()),
                file.getSize(),
                normalizeUploadedBy(uploadedBy)
        );

        RequestAttachment savedAttachment = requestAttachmentRepository.save(attachment);
        activityEventService.recordFileUploaded(savedAttachment);

        return RequestAttachmentResponse.from(savedAttachment);
    }

    @Transactional(readOnly = true)
    public RequestAttachmentDownload loadDownload(UUID id) {
        RequestAttachment attachment = findAttachment(id);
        Path filePath = UPLOAD_ROOT.resolve(attachment.getStoredFileName()).normalize();

        if (!filePath.startsWith(UPLOAD_ROOT)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid file path");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(NOT_FOUND, "Attachment file not found");
            }

            return new RequestAttachmentDownload(attachment, resource);
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Attachment file could not be loaded", exception);
        }
    }

    public String contentDispositionValue(RequestAttachment attachment) {
        String encodedFileName = UriUtils.encode(attachment.getOriginalFileName(), StandardCharsets.UTF_8);
        return "attachment; filename*=UTF-8''" + encodedFileName;
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = Path.of(fileName == null ? "attachment" : fileName)
                .getFileName()
                .toString()
                .trim()
                .replaceAll("[\\r\\n]", "");

        if (sanitized.isBlank()) {
            return "attachment";
        }

        return sanitized.length() > 255 ? sanitized.substring(sanitized.length() - 255) : sanitized;
    }

    private String extensionFrom(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex);
    }

    private String normalizeContentType(String contentType) {
        String trimmed = contentType == null ? "" : contentType.trim();
        return trimmed.isBlank() ? "application/octet-stream" : trimmed;
    }

    private String normalizeUploadedBy(String uploadedBy) {
        String trimmed = uploadedBy == null ? "" : uploadedBy.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private RequestAttachment findAttachment(UUID id) {
        return requestAttachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Attachment not found"));
    }

    private WorkRequest findWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
    }
}
