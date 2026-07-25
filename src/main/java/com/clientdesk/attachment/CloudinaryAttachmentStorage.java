package com.clientdesk.attachment;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

public class CloudinaryAttachmentStorage implements AttachmentStorage {

    private static final String RESOURCE_TYPE = "raw";
    private static final String DELIVERY_TYPE = "authenticated";

    private final Cloudinary cloudinary;
    private final AttachmentProperties properties;
    private final HttpClient httpClient;
    private final String folderPrefix;

    CloudinaryAttachmentStorage(
            Cloudinary cloudinary,
            AttachmentProperties properties,
            HttpClient httpClient
    ) {
        this.cloudinary = cloudinary;
        this.properties = properties;
        this.httpClient = httpClient;
        this.folderPrefix = normalizePrefix(properties.getCloudinary().getFolderPrefix());
    }

    @Override
    public void store(String storageKey, byte[] content) throws IOException {
        String publicId = publicId(storageKey);
        try {
            cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "resource_type", RESOURCE_TYPE,
                    "type", DELIVERY_TYPE,
                    "public_id", publicId,
                    "overwrite", false,
                    "unique_filename", false,
                    "use_filename", false
            ));
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Attachment could not be stored", exception);
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        String signedUrl;
        try {
            signedUrl = cloudinary.privateDownload(
                    publicId(storageKey),
                    null,
                    ObjectUtils.asMap(
                            "resource_type", RESOURCE_TYPE,
                            "type", DELIVERY_TYPE,
                            "attachment", false,
                            "expires_at", Instant.now().plusSeconds(30).getEpochSecond()
                    )
            );
        } catch (Exception exception) {
            throw new IOException("Attachment download could not be authorized", exception);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
                .timeout(properties.getCloudinary().getDownloadTimeout())
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                throw new FileNotFoundException("Attachment file not found");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Attachment download failed");
            }
            byte[] content = response.body();
            if (content == null || content.length > properties.getMaxFileBytes()) {
                throw new IOException("Attachment download returned an invalid response");
            }
            return new ByteArrayResource(content);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Attachment download was interrupted", exception);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Attachment download URL was invalid", exception);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            cloudinary.uploader().destroy(publicId(storageKey), ObjectUtils.asMap(
                    "resource_type", RESOURCE_TYPE,
                    "type", DELIVERY_TYPE,
                    "invalidate", true
            ));
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Attachment could not be deleted", exception);
        }
    }

    private String publicId(String storageKey) throws IOException {
        if (storageKey == null
                || storageKey.isBlank()
                || storageKey.startsWith("/")
                || storageKey.contains("\\")
                || storageKey.contains("..")) {
            throw new IOException("Invalid attachment storage key");
        }
        return folderPrefix + "/" + storageKey;
    }

    private String normalizePrefix(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank() || normalized.contains("..") || normalized.contains("\\")) {
            throw new IllegalStateException("CLOUDINARY_FOLDER_PREFIX is invalid");
        }
        return normalized;
    }
}
