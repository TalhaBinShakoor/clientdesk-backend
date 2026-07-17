package com.clientdesk.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.clientdesk.comment.Comment;
import com.clientdesk.comment.CommentRepository;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class AiAssistantService {

    private final WorkRequestRepository workRequestRepository;
    private final CommentRepository commentRepository;
    private final RestClient openAiClient;
    private final String openAiApiKey;
    private final String openAiModel;

    public AiAssistantService(
            WorkRequestRepository workRequestRepository,
            CommentRepository commentRepository,
            RestClient.Builder restClientBuilder,
            @Value("${clientdesk.ai.openai.api-key:}") String openAiApiKey,
            @Value("${clientdesk.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            @Value("${clientdesk.ai.openai.model:gpt-5-nano}") String openAiModel
    ) {
        this.workRequestRepository = workRequestRepository;
        this.commentRepository = commentRepository;
        this.openAiClient = restClientBuilder.baseUrl(openAiBaseUrl).build();
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
    }

    public AiAssistantResponse summarizeRequestThread(UUID workRequestId) {
        WorkRequest workRequest = findWorkRequest(workRequestId);
        List<Comment> comments = commentRepository.findByWorkRequest_IdOrderByCreatedAtAsc(workRequestId);
        String fallbackSummary = buildLocalSummary(workRequest, comments);
        String prompt = """
                Summarize this client work request thread for an admin user.

                Focus on:
                - the client need
                - current status and urgency
                - useful conversation details
                - the best next step

                Use a clear business tone. Keep the output practical and concise.

                Request context:
                %s
                """.formatted(buildRequestContext(workRequest, comments));

        return new AiAssistantResponse(generateWithOpenAi(prompt, fallbackSummary));
    }

    public AiAssistantResponse draftClientReply(UUID workRequestId, DraftClientReplyRequest request) {
        WorkRequest workRequest = findWorkRequest(workRequestId);
        List<Comment> comments = commentRepository.findByWorkRequest_IdOrderByCreatedAtAsc(workRequestId);
        String tone = fallback(request == null ? null : request.tone(), "professional");
        String fallbackDraft = buildLocalDraft(workRequest, comments, tone);
        String prompt = """
                Draft a client-facing reply for this work request.

                Tone: %s

                Requirements:
                - sound human, concise, and helpful
                - acknowledge the request and latest context
                - explain the next step
                - do not invent dates, prices, or promises
                - return only the message body

                Request context:
                %s
                """.formatted(tone, buildRequestContext(workRequest, comments));

        return new AiAssistantResponse(generateWithOpenAi(prompt, fallbackDraft));
    }

    private String buildLocalSummary(WorkRequest workRequest, List<Comment> comments) {
        return """
                Request summary

                Client: %s
                Title: %s
                Status: %s
                Priority: %s
                Requested by: %s
                Due date: %s

                Main need:
                %s

                Conversation so far:
                %s

                Recommended next step:
                %s
                """.formatted(
                workRequest.getClient().getCompanyName(),
                workRequest.getTitle(),
                workRequest.getStatus(),
                workRequest.getPriority(),
                fallback(workRequest.getRequestedBy(), "Not provided"),
                workRequest.getDueDate() == null ? "Not set" : workRequest.getDueDate(),
                fallback(workRequest.getDescription(), "No description has been added yet."),
                summarizeComments(comments),
                recommendedNextStep(workRequest)
        ).trim();
    }

    private String buildLocalDraft(WorkRequest workRequest, List<Comment> comments, String tone) {
        return """
                Hi %s,

                Thanks for the update on "%s". I have reviewed the request details and the latest conversation.

                Current status: %s
                Priority: %s

                %s

                Next, I will %s.

                Best,
                ClientDesk Team
                """.formatted(
                fallback(workRequest.getRequestedBy(), "there"),
                workRequest.getTitle(),
                readable(workRequest.getStatus().name()),
                readable(workRequest.getPriority().name()),
                clientFacingContext(comments, tone),
                recommendedNextStep(workRequest)
        ).trim();
    }

    private WorkRequest findWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
    }

    private String generateWithOpenAi(String prompt, String fallbackContent) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackContent;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", openAiModel);
            requestBody.put("instructions", """
                    You are ClientDesk's AI assistant for a client services dashboard.
                    Help admins summarize request threads and draft client replies using only the provided context.
                    """);
            requestBody.put("input", prompt);

            JsonNode response = openAiClient.post()
                    .uri("/responses")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String generatedContent = extractGeneratedContent(response);
            return generatedContent.isBlank() ? fallbackContent : generatedContent;
        } catch (RestClientException exception) {
            return fallbackContent;
        }
    }

    private String extractGeneratedContent(JsonNode response) {
        if (response == null || response.isMissingNode()) {
            return "";
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText().trim();
        }

        List<String> textParts = new ArrayList<>();
        for (JsonNode outputItem : response.path("output")) {
            for (JsonNode contentItem : outputItem.path("content")) {
                JsonNode text = contentItem.path("text");
                if (text.isTextual()) {
                    textParts.add(text.asText());
                }
            }
        }

        return String.join("\n", textParts).trim();
    }

    private String buildRequestContext(WorkRequest workRequest, List<Comment> comments) {
        return """
                Client: %s
                Title: %s
                Description: %s
                Status: %s
                Priority: %s
                Requested by: %s
                Due date: %s
                Comments:
                %s
                """.formatted(
                workRequest.getClient().getCompanyName(),
                workRequest.getTitle(),
                fallback(workRequest.getDescription(), "No description has been added yet."),
                readable(workRequest.getStatus().name()),
                readable(workRequest.getPriority().name()),
                fallback(workRequest.getRequestedBy(), "Not provided"),
                workRequest.getDueDate() == null ? "Not set" : workRequest.getDueDate(),
                summarizeComments(comments)
        ).trim();
    }

    private String summarizeComments(List<Comment> comments) {
        if (comments.isEmpty()) {
            return "No comments have been added yet.";
        }

        return String.join("\n", comments.stream()
                .map(comment -> "- %s: %s".formatted(comment.getAuthorName(), comment.getBody()))
                .toList());
    }

    private String recommendedNextStep(WorkRequest workRequest) {
        return switch (workRequest.getStatus()) {
            case NEW -> "review the request and confirm scope with the client";
            case IN_PROGRESS -> "continue the active work and share the next progress update";
            case WAITING_ON_CLIENT -> "follow up with the client for the information needed to proceed";
            case RESOLVED -> "confirm that the client is satisfied before closing the request";
            case CLOSED -> "keep the request closed unless the client reports a new issue";
        };
    }

    private String clientFacingContext(List<Comment> comments, String tone) {
        if (comments.isEmpty()) {
            return "I understand the request and will use the details provided to move it forward.";
        }

        Comment latestComment = comments.get(comments.size() - 1);

        if ("friendly".equalsIgnoreCase(tone)) {
            return "I also saw your latest note: \"%s\". That gives me a clear direction for the next update."
                    .formatted(latestComment.getBody());
        }

        return "I noted the latest comment: \"%s\". I will use that context when preparing the next update."
                .formatted(latestComment.getBody());
    }

    private String readable(String value) {
        String lower = value.toLowerCase().replace('_', ' ');
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
