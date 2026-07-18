package com.clientdesk.attachment;

import com.clientdesk.identity.AppUser;
import com.clientdesk.workrequest.WorkRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_attachments")
public class RequestAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private AppUser uploadedByUser;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "stored_file_name", nullable = false, unique = true, length = 255)
    private String storedFileName;

    @Size(max = 255)
    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Size(max = 200)
    @Column(name = "uploaded_by", length = 200)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequestAttachment() {
    }

    public RequestAttachment(
            WorkRequest workRequest,
            AppUser uploadedByUser,
            String originalFileName,
            String storedFileName,
            String contentType,
            long sizeBytes,
            String uploadedBy
    ) {
        this.workRequest = workRequest;
        this.uploadedByUser = uploadedByUser;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public AppUser getUploadedByUser() {
        return uploadedByUser;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
