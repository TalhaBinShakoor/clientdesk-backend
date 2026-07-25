package com.clientdesk.attachment;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryAttachmentStorageTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private HttpClient httpClient;
    private CloudinaryAttachmentStorage storage;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        httpClient = mock(HttpClient.class);

        AttachmentProperties properties = new AttachmentProperties();
        properties.getCloudinary().setFolderPrefix("clientdesk/attachments");
        properties.getCloudinary().setDownloadTimeout(Duration.ofSeconds(2));

        when(cloudinary.uploader()).thenReturn(uploader);
        storage = new CloudinaryAttachmentStorage(cloudinary, properties, httpClient);
    }

    @Test
    void storesAuthenticatedRawAssetWithoutOverwrite() throws Exception {
        byte[] content = "safe attachment".getBytes();
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("result", "ok"));

        storage.store("organization/request/attachment.txt", content);

        verify(uploader).upload(eq(content), org.mockito.ArgumentMatchers.argThat(options ->
                "raw".equals(options.get("resource_type"))
                        && "authenticated".equals(options.get("type"))
                        && "clientdesk/attachments/organization/request/attachment.txt"
                        .equals(options.get("public_id"))
                        && Boolean.FALSE.equals(options.get("overwrite"))
        ));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loadsThroughShortLivedSignedUrl() throws Exception {
        byte[] content = "downloaded".getBytes();
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(cloudinary.privateDownload(
                eq("clientdesk/attachments/organization/request/attachment.txt"),
                isNull(),
                anyMap()
        )).thenReturn("https://api.cloudinary.test/download");
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(content);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) response);

        assertThat(storage.load("organization/request/attachment.txt").getContentAsByteArray())
                .isEqualTo(content);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsMissingRemoteAssetToFileNotFound() throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(cloudinary.privateDownload(any(), isNull(), anyMap()))
                .thenReturn("https://api.cloudinary.test/missing");
        when(response.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) response);

        assertThatThrownBy(() -> storage.load("organization/request/missing.txt"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void deletesMatchingAuthenticatedRawAsset() throws Exception {
        when(uploader.destroy(any(), anyMap())).thenReturn(Map.of("result", "ok"));

        storage.delete("organization/request/attachment.txt");

        verify(uploader).destroy(
                eq("clientdesk/attachments/organization/request/attachment.txt"),
                org.mockito.ArgumentMatchers.argThat(options ->
                        "raw".equals(options.get("resource_type"))
                                && "authenticated".equals(options.get("type"))
                                && Boolean.TRUE.equals(options.get("invalidate"))
                )
        );
    }

    @Test
    void rejectsUnsafeStorageKey() {
        assertThatThrownBy(() -> storage.store("../outside.txt", new byte[]{1}))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid attachment storage key");
    }
}
