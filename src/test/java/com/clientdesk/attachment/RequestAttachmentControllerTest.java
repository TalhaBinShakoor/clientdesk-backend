package com.clientdesk.attachment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin@clientdesk.test")
class RequestAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadListDownloadAndRecordActivity() throws Exception {
        String clientId = createClient("Attachment Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Request with attachment " + UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "brief.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Please review the launch brief.".getBytes()
        );

        String responseBody = mockMvc.perform(multipart("/api/request-attachments")
                        .file(file)
                        .with(csrf())
                        .param("workRequestId", workRequestId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workRequestId").value(workRequestId))
                .andExpect(jsonPath("$.originalFileName").value("brief.txt"))
                .andExpect(jsonPath("$.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(jsonPath("$.sizeBytes").value(31))
                .andExpect(jsonPath("$.uploadedBy").value("Alex Morgan"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String attachmentId = extractId(responseBody);

        mockMvc.perform(get("/api/request-attachments").param("workRequestId", workRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(attachmentId)))
                .andExpect(jsonPath("$.content[*].originalFileName", hasItem("brief.txt")));

        mockMvc.perform(get("/api/request-attachments/{id}/download", attachmentId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"=?UTF-8?Q?brief.txt?=\"; filename*=UTF-8''brief.txt"
                ))
                .andExpect(header().string("Content-Length", "31"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Security-Policy", "sandbox; default-src 'none'"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("X-Download-Options", "noopen"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Please review the launch brief."));

        mockMvc.perform(get("/api/activity-events").param("workRequestId", workRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].eventType", hasItem("FILE_UPLOADED")))
                .andExpect(jsonPath("$.content[*].summary", hasItem("File uploaded: brief.txt")));
    }

    @Test
    void uploadRequiresFile() throws Exception {
        String clientId = createClient("Attachment Validation Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Request missing attachment " + UUID.randomUUID());

        mockMvc.perform(multipart("/api/request-attachments")
                        .with(csrf())
                        .param("workRequestId", workRequestId)
                        .param("uploadedBy", "Alex Morgan"))
                .andExpect(status().isBadRequest());
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

    private String createWorkRequest(String clientId, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/work-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "status": "NEW",
                                  "priority": "HIGH",
                                  "requestedBy": "Alex Morgan"
                                }
                                """.formatted(clientId, title)))
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
