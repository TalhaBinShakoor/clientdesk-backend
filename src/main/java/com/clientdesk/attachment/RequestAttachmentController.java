package com.clientdesk.attachment;

import com.clientdesk.api.ApiPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@Validated
@RequestMapping("/api/request-attachments")
public class RequestAttachmentController {

    private final RequestAttachmentService requestAttachmentService;

    public RequestAttachmentController(RequestAttachmentService requestAttachmentService) {
        this.requestAttachmentService = requestAttachmentService;
    }

    @GetMapping
    public ApiPage<RequestAttachmentResponse> findAll(
            @RequestParam UUID workRequestId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return requestAttachmentService.findAll(workRequestId, page, size);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public RequestAttachmentResponse upload(
            @RequestParam UUID workRequestId,
            @RequestParam MultipartFile file
    ) {
        return requestAttachmentService.upload(workRequestId, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        RequestAttachmentDownload download = requestAttachmentService.loadDownload(id);
        RequestAttachment attachment = download.attachment();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(attachment.getSizeBytes())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, requestAttachmentService.contentDispositionValue(attachment))
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .header("Cross-Origin-Resource-Policy", "same-origin")
                .header("X-Download-Options", "noopen")
                .body(download.resource());
    }
}
