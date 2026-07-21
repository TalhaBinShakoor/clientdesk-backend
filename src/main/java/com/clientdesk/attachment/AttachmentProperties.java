package com.clientdesk.attachment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@Validated
@ConfigurationProperties(prefix = "clientdesk.attachments")
public class AttachmentProperties {

    @NotNull
    private Path storageRoot = Path.of("uploads", "request-attachments");

    @Min(1)
    private long maxFileBytes = 5_242_880;

    @Min(1)
    private int maxFilesPerWorkRequest = 20;

    @Min(1)
    private long maxBytesPerOrganization = 52_428_800;

    @NotEmpty
    private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "text/csv"
    ));

    public Path getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }

    public int getMaxFilesPerWorkRequest() {
        return maxFilesPerWorkRequest;
    }

    public void setMaxFilesPerWorkRequest(int maxFilesPerWorkRequest) {
        this.maxFilesPerWorkRequest = maxFilesPerWorkRequest;
    }

    public long getMaxBytesPerOrganization() {
        return maxBytesPerOrganization;
    }

    public void setMaxBytesPerOrganization(long maxBytesPerOrganization) {
        this.maxBytesPerOrganization = maxBytesPerOrganization;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(Set<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
