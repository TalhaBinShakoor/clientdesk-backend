package com.clientdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clientdesk.api.rate-limit.enabled=true",
        "clientdesk.api.rate-limit.window-seconds=60",
        "clientdesk.api.rate-limit.max-tracked-keys=100",
        "clientdesk.api.rate-limit.write.max-per-user=2",
        "clientdesk.api.rate-limit.write.max-per-organization=20",
        "clientdesk.api.rate-limit.upload.max-per-user=1",
        "clientdesk.api.rate-limit.upload.max-per-organization=20",
        "clientdesk.api.rate-limit.download.max-per-user=1",
        "clientdesk.api.rate-limit.download.max-per-organization=20",
        "clientdesk.api.rate-limit.ai.max-per-user=1",
        "clientdesk.api.rate-limit.ai.max-per-organization=20"
})
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("admin@clientdesk.test")
class ApiRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void limitsWritesByAuthenticatedUserDespiteSpoofedForwardingHeaders() throws Exception {
        createClient("198.51.100.1").andExpect(status().isCreated());
        createClient("198.51.100.2").andExpect(status().isCreated());

        createClient("198.51.100.3")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9][0-9]?")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Request rate limit exceeded. Try again later"));
    }

    @Test
    void appliesSeparateUploadLimit() throws Exception {
        uploadWithoutFile().andExpect(status().isBadRequest());
        uploadWithoutFile()
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void appliesSeparateDownloadLimit() throws Exception {
        String attachmentId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/request-attachments/{id}/download", attachmentId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/request-attachments/{id}/download", attachmentId))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void appliesSeparateAiLimit() throws Exception {
        String workRequestId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/ai-assistant/work-requests/{id}/summary", workRequestId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ai-assistant/work-requests/{id}/summary", workRequestId))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    private org.springframework.test.web.servlet.ResultActions createClient(String forwardedFor) throws Exception {
        return mockMvc.perform(post("/api/clients")
                .with(csrf())
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "companyName": "Rate Limited Client %s"
                        }
                        """.formatted(UUID.randomUUID())));
    }

    private org.springframework.test.web.servlet.ResultActions uploadWithoutFile() throws Exception {
        return mockMvc.perform(post("/api/request-attachments")
                .with(csrf())
                .param("workRequestId", UUID.randomUUID().toString()));
    }
}
