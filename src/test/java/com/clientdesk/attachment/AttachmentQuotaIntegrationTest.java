package com.clientdesk.attachment;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clientdesk.attachments.max-file-bytes=100",
        "clientdesk.attachments.max-files-per-work-request=1",
        "clientdesk.attachments.max-bytes-per-organization=20"
})
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin@clientdesk.test")
class AttachmentQuotaIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "clientdesk-attachment-quota-" + UUID.randomUUID()
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestAttachmentRepository requestAttachmentRepository;

    @DynamicPropertySource
    static void attachmentStorage(DynamicPropertyRegistry registry) {
        registry.add("clientdesk.attachments.storage-root", STORAGE_ROOT::toString);
    }

    @BeforeEach
    void isolateOrganizationQuota() {
        requestAttachmentRepository.deleteAllInBatch();
    }

    @AfterAll
    static void removeStorageRoot() throws IOException {
        if (!Files.exists(STORAGE_ROOT)) {
            return;
        }
        try (var paths = Files.walk(STORAGE_ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void enforcesWorkRequestAttachmentCount() throws Exception {
        String workRequestId = createWorkRequest("Count quota");

        upload(workRequestId, "first.txt", "one".getBytes()).andExpect(status().isCreated());
        upload(workRequestId, "second.txt", "two".getBytes()).andExpect(status().isConflict());
    }

    @Test
    void enforcesOrganizationStorageQuotaAcrossWorkRequests() throws Exception {
        String firstWorkRequestId = createWorkRequest("Organization quota one");
        String secondWorkRequestId = createWorkRequest("Organization quota two");

        upload(firstWorkRequestId, "first.txt", "123456789012".getBytes())
                .andExpect(status().isCreated());
        upload(secondWorkRequestId, "second.txt", "123456789012".getBytes())
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void unicodeFilenameIsEncodedSafelyForDownload() throws Exception {
        String workRequestId = createWorkRequest("Unicode filename");
        String attachmentId = extractId(upload(
                workRequestId,
                "r\u00e9sum\u00e9.txt",
                "safe".getBytes(StandardCharsets.UTF_8)
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/request-attachments/{id}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("filename*=UTF-8''r%C3%A9sum%C3%A9.txt")
                ));
    }

    private org.springframework.test.web.servlet.ResultActions upload(
            String workRequestId,
            String fileName,
            byte[] content
    ) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content
        );
        return mockMvc.perform(multipart("/api/request-attachments")
                .file(file)
                .with(csrf())
                .param("workRequestId", workRequestId));
    }

    private String createWorkRequest(String label) throws Exception {
        String clientId = createClient(label + " client " + UUID.randomUUID());
        String responseBody = mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s request",
                                  "status": "NEW",
                                  "priority": "HIGH"
                                }
                                """.formatted(clientId, label)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(responseBody);
    }

    private String createClient(String companyName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "%s",
                                  "status": "ACTIVE"
                                }
                                """.formatted(companyName)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(responseBody);
    }

    private String extractId(String responseBody) {
        return responseBody.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
