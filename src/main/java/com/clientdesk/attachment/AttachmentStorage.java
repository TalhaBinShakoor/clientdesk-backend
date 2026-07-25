package com.clientdesk.attachment;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface AttachmentStorage {

    void store(String storageKey, byte[] content) throws IOException;

    Resource load(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
