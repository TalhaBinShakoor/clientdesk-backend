package com.clientdesk.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndListWorkRequestComments() throws Exception {
        String clientId = createClient("Comment Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Commented request " + UUID.randomUUID());
        String commentBody = "Please confirm the copy direction.";

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRequestId": "%s",
                                  "authorName": "Alex Morgan",
                                  "body": "%s"
                                }
                                """.formatted(workRequestId, commentBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workRequestId").value(workRequestId))
                .andExpect(jsonPath("$.projectTaskId").doesNotExist())
                .andExpect(jsonPath("$.authorName").value("Alex Morgan"))
                .andExpect(jsonPath("$.body").value(commentBody));

        mockMvc.perform(get("/api/comments").param("workRequestId", workRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body", hasItem(commentBody)));
    }

    @Test
    void createAndListProjectTaskComments() throws Exception {
        String clientId = createClient("Task Comment Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Request with task comment " + UUID.randomUUID());
        String projectTaskId = createProjectTask(workRequestId, "Write launch checklist " + UUID.randomUUID());
        String commentBody = "Checklist draft is ready for review.";

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectTaskId": "%s",
                                  "authorName": "Nina Patel",
                                  "body": "%s"
                                }
                                """.formatted(projectTaskId, commentBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workRequestId").doesNotExist())
                .andExpect(jsonPath("$.projectTaskId").value(projectTaskId))
                .andExpect(jsonPath("$.authorName").value("Nina Patel"))
                .andExpect(jsonPath("$.body").value(commentBody));

        mockMvc.perform(get("/api/comments").param("projectTaskId", projectTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body", hasItem(commentBody)));
    }

    @Test
    void commentsRequireExactlyOneTarget() throws Exception {
        String clientId = createClient("Invalid Comment Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Invalid comment request " + UUID.randomUUID());
        String projectTaskId = createProjectTask(workRequestId, "Invalid comment task " + UUID.randomUUID());

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRequestId": "%s",
                                  "projectTaskId": "%s",
                                  "authorName": "Alex Morgan",
                                  "body": "This should fail."
                                }
                                """.formatted(workRequestId, projectTaskId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/comments"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestTimelineIncludesCreatesStatusChangesAndComments() throws Exception {
        String clientId = createClient("Timeline Client " + UUID.randomUUID());
        String workRequestId = createWorkRequest(clientId, "Timeline request " + UUID.randomUUID());
        String projectTaskId = createProjectTask(workRequestId, "Timeline task " + UUID.randomUUID());

        mockMvc.perform(patch("/api/work-requests/{id}/status", workRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/project-tasks/{id}/status", projectTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectTaskId": "%s",
                                  "authorName": "Alex Morgan",
                                  "body": "Task is complete."
                                }
                                """.formatted(projectTaskId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/activity-events").param("workRequestId", workRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType", hasItems(
                        "WORK_REQUEST_CREATED",
                        "PROJECT_TASK_CREATED",
                        "WORK_REQUEST_STATUS_CHANGED",
                        "PROJECT_TASK_STATUS_CHANGED",
                        "COMMENT_ADDED"
                )))
                .andExpect(jsonPath("$[*].workRequestId", hasItem(workRequestId)))
                .andExpect(jsonPath("$[*].projectTaskId", hasItem(projectTaskId)))
                .andExpect(jsonPath("$[*].summary", hasItem("Comment added")));
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

    private String createProjectTask(String workRequestId, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/project-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRequestId": "%s",
                                  "title": "%s",
                                  "status": "TODO",
                                  "assignee": "Nina Patel"
                                }
                                """.formatted(workRequestId, title)))
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
