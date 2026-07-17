package com.clientdesk.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void summarizeRequestThread() throws Exception {
        String clientId = createClient("AI Summary Client " + UUID.randomUUID());
        String workRequestTitle = "Summarize onboarding request " + UUID.randomUUID();
        String workRequestId = createWorkRequest(clientId, workRequestTitle);

        createComment(workRequestId, "Alex Morgan", "Please confirm the launch copy direction.");
        createComment(workRequestId, "Nina Patel", "Draft copy is ready for review.");

        mockMvc.perform(get("/api/ai-assistant/work-requests/{workRequestId}/summary", workRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("Request summary")))
                .andExpect(jsonPath("$.content", containsString(workRequestTitle)))
                .andExpect(jsonPath("$.content", containsString("Please confirm the launch copy direction.")))
                .andExpect(jsonPath("$.content", containsString("Recommended next step")));
    }

    @Test
    void draftClientReply() throws Exception {
        String clientId = createClient("AI Draft Client " + UUID.randomUUID());
        String workRequestTitle = "Draft reply request " + UUID.randomUUID();
        String workRequestId = createWorkRequest(clientId, workRequestTitle);

        createComment(workRequestId, "Alex Morgan", "Can you send the next client update?");

        mockMvc.perform(post("/api/ai-assistant/work-requests/{workRequestId}/draft-reply", workRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tone": "friendly"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("Hi Alex Morgan")))
                .andExpect(jsonPath("$.content", containsString(workRequestTitle)))
                .andExpect(jsonPath("$.content", containsString("Can you send the next client update?")))
                .andExpect(jsonPath("$.content", containsString("Next, I will")));
    }

    @Test
    void returnsNotFoundForMissingWorkRequest() throws Exception {
        mockMvc.perform(get("/api/ai-assistant/work-requests/{workRequestId}/summary", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private String createClient(String companyName) throws Exception {
        String responseBody = mockMvc.perform(post("/api/clients")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "description": "Prepare a polished onboarding update for the client.",
                                  "status": "IN_PROGRESS",
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

    private void createComment(String workRequestId, String authorName, String body) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRequestId": "%s",
                                  "authorName": "%s",
                                  "body": "%s"
                                }
                                """.formatted(workRequestId, authorName, body)))
                .andExpect(status().isCreated());
    }

    private String extractId(String responseBody) {
        return responseBody.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
