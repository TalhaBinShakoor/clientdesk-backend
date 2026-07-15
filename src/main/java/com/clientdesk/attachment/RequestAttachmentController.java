package com.clientdesk.attachment;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/request-attachments")
public class RequestAttachmentController {

    private final RequestAttachmentService requestAttachmentService;

    public RequestAttachmentController(RequestAttachmentService requestAttachmentService) {
        this.requestAttachmentService = requestAttachmentService;
    }

    @GetMapping
    public List<RequestAttachmentResponse> findAll(@RequestParam UUID workRequestId) {
        return requestAttachmentService.findAll(workRequestId);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public RequestAttachmentResponse upload(
            @RequestParam UUID workRequestId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String uploadedBy
    ) {
        return requestAttachmentService.upload(workRequestId, file, uploadedBy);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        RequestAttachmentDownload download = requestAttachmentService.loadDownload(id);
        RequestAttachment attachment = download.attachment();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, requestAttachmentService.contentDispositionValue(attachment))
                .body(download.resource());
    }
}
