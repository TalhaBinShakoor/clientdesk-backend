package com.clientdesk.attachment;

import org.junit.jupiter.api.AfterAll;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin@clientdesk.test")
class AttachmentDeletionCleanupIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of(
            System.getProperty("java.io.tmpdir"),
            "clientdesk-attachment-deletion-" + UUID.randomUUID()
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestAttachmentRepository requestAttachmentRepository;

    @DynamicPropertySource
    static void attachmentStorage(DynamicPropertyRegistry registry) {
        registry.add("clientdesk.attachments.storage-root", STORAGE_ROOT::toString);
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
    void deletingWorkRequestRemovesAttachmentFileAfterCommit() throws Exception {
        UUID clientId = createClient("Request cleanup " + UUID.randomUUID());
        UUID workRequestId = createWorkRequest(clientId, "Delete request attachments");
        upload(workRequestId, "request-cleanup.txt");

        String storedFileName = requestAttachmentRepository
                .findStoredFileNamesByWorkRequestId(workRequestId)
                .getFirst();
        Path storedFile = STORAGE_ROOT.resolve(storedFileName);
        assertThat(storedFile).exists();

        mockMvc.perform(delete("/api/work-requests/{id}", workRequestId).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(storedFile).doesNotExist();
        assertThat(requestAttachmentRepository.findStoredFileNamesByWorkRequestId(workRequestId)).isEmpty();
    }

    @Test
    void deletingClientRemovesAttachmentFilesFromCascadedRequestsAfterCommit() throws Exception {
        UUID clientId = createClient("Client cleanup " + UUID.randomUUID());
        UUID firstRequestId = createWorkRequest(clientId, "First client attachment");
        UUID secondRequestId = createWorkRequest(clientId, "Second client attachment");
        upload(firstRequestId, "first-client-cleanup.txt");
        upload(secondRequestId, "second-client-cleanup.txt");

        List<Path> storedFiles = requestAttachmentRepository.findStoredFileNamesByClientId(clientId)
                .stream()
                .map(STORAGE_ROOT::resolve)
                .toList();
        assertThat(storedFiles).hasSize(2).allMatch(Files::exists);

        mockMvc.perform(delete("/api/clients/{id}", clientId).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(storedFiles).allMatch(path -> !Files.exists(path));
        assertThat(requestAttachmentRepository.findStoredFileNamesByClientId(clientId)).isEmpty();
    }

    private UUID createClient(String companyName) throws Exception {
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
        return UUID.fromString(extractId(responseBody));
    }

    private UUID createWorkRequest(UUID clientId, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "status": "NEW",
                                  "priority": "MEDIUM"
                                }
                                """.formatted(clientId, title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(extractId(responseBody));
    }

    private void upload(UUID workRequestId, String fileName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                "cleanup test".getBytes()
        );
        mockMvc.perform(multipart("/api/request-attachments")
                        .file(file)
                        .with(csrf())
                        .param("workRequestId", workRequestId.toString()))
                .andExpect(status().isCreated());
    }

    private String extractId(String responseBody) {
        return responseBody.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
