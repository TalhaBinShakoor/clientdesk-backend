package com.clientdesk.workrequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListWorkRequests() throws Exception {
        String clientId = createClient("Request Client " + UUID.randomUUID());
        String title = "Launch landing page " + UUID.randomUUID();

        mockMvc.perform(post("/api/work-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "description": "Build a marketing landing page",
                                  "status": "NEW",
                                  "priority": "HIGH",
                                  "requestedBy": "Alex Morgan",
                                  "dueDate": "2026-08-15"
                                }
                                """.formatted(clientId, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.clientCompanyName").exists())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.requestedBy").value("Alex Morgan"))
                .andExpect(jsonPath("$.dueDate").value("2026-08-15"));

        mockMvc.perform(get("/api/work-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(title)));
    }

    @Test
    void filterWorkRequestsByStatusPriorityAndClient() throws Exception {
        String firstClientId = createClient("Filter Client A " + UUID.randomUUID());
        String secondClientId = createClient("Filter Client B " + UUID.randomUUID());
        String matchingTitle = "Matching request " + UUID.randomUUID();
        String otherTitle = "Other request " + UUID.randomUUID();

        createWorkRequest(firstClientId, matchingTitle, "IN_PROGRESS", "URGENT");
        createWorkRequest(secondClientId, otherTitle, "NEW", "LOW");

        mockMvc.perform(get("/api/work-requests")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "URGENT")
                        .param("clientId", firstClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(matchingTitle)))
                .andExpect(jsonPath("$[*].title", not(hasItem(otherTitle))));
    }

    @Test
    void updateStatusAndDeleteWorkRequest() throws Exception {
        String clientId = createClient("Status Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(
                clientId,
                "Fix checkout issue " + UUID.randomUUID(),
                "NEW",
                "HIGH"
        );

        mockMvc.perform(patch("/api/work-requests/{id}/status", workRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(delete("/api/work-requests/{id}", workRequestId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/work-requests/{id}", workRequestId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWorkRequestDetails() throws Exception {
        String firstClientId = createClient("Original Client " + UUID.randomUUID());
        String secondClientId = createClient("Updated Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(
                firstClientId,
                "Original ticket " + UUID.randomUUID(),
                "NEW",
                "MEDIUM"
        );

        mockMvc.perform(put("/api/work-requests/{id}", workRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "Updated onboarding request",
                                  "description": "Updated ticket details",
                                  "status": "WAITING_ON_CLIENT",
                                  "priority": "LOW",
                                  "requestedBy": "Nina Patel",
                                  "dueDate": "2026-09-01"
                                }
                                """.formatted(secondClientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(secondClientId))
                .andExpect(jsonPath("$.title").value("Updated onboarding request"))
                .andExpect(jsonPath("$.description").value("Updated ticket details"))
                .andExpect(jsonPath("$.status").value("WAITING_ON_CLIENT"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.requestedBy").value("Nina Patel"))
                .andExpect(jsonPath("$.dueDate").value("2026-09-01"));
    }

    @Test
    void createWorkRequestRequiresClientAndTitle() throws Exception {
        mockMvc.perform(post("/api/work-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWorkRequestReturnsNotFoundForMissingClient() throws Exception {
        mockMvc.perform(post("/api/work-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "Request for missing client"
                                }
                                """.formatted(UUID.randomUUID())))
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

    private String createWorkRequest(
            String clientId,
            String title,
            String status,
            String priority
    ) throws Exception {
        String responseBody = mockMvc.perform(post("/api/work-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "%s",
                                  "status": "%s",
                                  "priority": "%s"
                                }
                                """.formatted(clientId, title, status, priority)))
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
