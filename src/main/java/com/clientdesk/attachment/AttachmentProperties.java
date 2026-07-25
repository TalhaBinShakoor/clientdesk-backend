package com.clientdesk.attachment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@Validated
@ConfigurationProperties(prefix = "clientdesk.attachments")
public class AttachmentProperties {

    @NotEmpty
    private String storageProvider = "local";

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

    @NotNull
    private Cloudinary cloudinary = new Cloudinary();

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

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

    public Cloudinary getCloudinary() {
        return cloudinary;
    }

    public void setCloudinary(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public static class Cloudinary {

        private String cloudName = "";
        private String apiKey = "";
        private String apiSecret = "";

        @NotEmpty
        private String folderPrefix = "clientdesk/attachments";

        @NotNull
        private Duration downloadTimeout = Duration.ofSeconds(10);

        public String getCloudName() {
            return cloudName;
        }

        public void setCloudName(String cloudName) {
            this.cloudName = cloudName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public String getFolderPrefix() {
            return folderPrefix;
        }

        public void setFolderPrefix(String folderPrefix) {
            this.folderPrefix = folderPrefix;
        }

        public Duration getDownloadTimeout() {
            return downloadTimeout;
        }

        public void setDownloadTimeout(Duration downloadTimeout) {
            this.downloadTimeout = downloadTimeout;
        }
    }
}
