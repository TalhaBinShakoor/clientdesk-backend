package com.clientdesk.attachment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(
        prefix = "clientdesk.attachments",
        name = "storage-provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalAttachmentStorage implements AttachmentStorage {

    private final Path storageRoot;

    public LocalAttachmentStorage(AttachmentProperties properties) {
        this.storageRoot = properties.getStorageRoot().toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, byte[] content) throws IOException {
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("Attachment file not found");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IOException("Attachment file could not be loaded", exception);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        Files.deleteIfExists(path);

        Path parent = path.getParent();
        while (parent != null && !parent.equals(storageRoot) && parent.startsWith(storageRoot)) {
            try {
                Files.delete(parent);
            } catch (DirectoryNotEmptyException exception) {
                return;
            }
            parent = parent.getParent();
        }
    }

    private Path resolve(String storageKey) throws IOException {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IOException("Invalid attachment storage key");
        }
        return resolved;
    }
}
