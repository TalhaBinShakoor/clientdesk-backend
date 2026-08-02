package com.clientdesk.ai;

import com.clientdesk.client.Client;
import com.clientdesk.comment.Comment;
import com.clientdesk.comment.CommentRepository;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestPriority;
import com.clientdesk.workrequest.WorkRequestRepository;
import com.clientdesk.workrequest.WorkRequestStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantSafetyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private String providerResponse;

    @BeforeEach
    void startProvider() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/responses", this::respond);
        server.start();
    }

    @AfterEach
    void stopProvider() {
        server.stop(0);
    }

    @Test
    void disablesStorageBoundsContextAndTruncatesOutput() throws Exception {
        providerResponse = "{\"output_text\":\"1234567890EXTRA\"}";
        TestFixture fixture = fixture(1024, 2, 120, 10, 123);

        AiAssistantResponse response = fixture.service().summarizeRequestThread(fixture.workRequestId());

        JsonNode outboundRequest = objectMapper.readTree(requestBody.get());
        assertThat(outboundRequest.path("store").asBoolean()).isFalse();
        assertThat(outboundRequest.path("max_output_tokens").asInt()).isEqualTo(123);
        assertThat(outboundRequest.path("input").asText()).contains("untrusted data");
        assertThat(outboundRequest.path("input").asText()).hasSizeLessThan(1000);
        assertThat(response.content()).isEqualTo("1234567890");
        assertThat(response.source()).isEqualTo(AiAssistantSource.OPENAI);
        verify(fixture.commentRepository()).findByWorkRequest_IdOrderByCreatedAtDesc(
                eq(fixture.workRequestId()),
                any(Pageable.class)
        );
        verify(fixture.workRequestRepository(), never()).save(any());
    }

    @Test
    void oversizedProviderResponseFallsBackWithoutPersistingContent() {
        providerResponse = "{\"output_text\":\"" + "x".repeat(300) + "\"}";
        TestFixture fixture = fixture(128, 2, 20000, 1000, 123);

        AiAssistantResponse response = fixture.service().summarizeRequestThread(fixture.workRequestId());

        assertThat(response.content()).startsWith("Request summary");
        assertThat(response.source()).isEqualTo(AiAssistantSource.LOCAL_FALLBACK);
        verify(fixture.workRequestRepository(), never()).save(any());
    }

    private TestFixture fixture(
            long maxResponseBytes,
            int maxContextComments,
            int maxContextCharacters,
            int maxOutputCharacters,
            int maxOutputTokens
    ) {
        UUID workRequestId = UUID.randomUUID();
        WorkRequestRepository workRequestRepository = mock(WorkRequestRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        AccessService accessService = mock(AccessService.class);
        WorkRequest workRequest = mock(WorkRequest.class);
        Client client = mock(Client.class);
        Comment firstComment = comment("First", "Earlier context");
        Comment secondComment = comment("Second", "Latest context");

        when(workRequestRepository.findById(workRequestId)).thenReturn(Optional.of(workRequest));
        when(workRequest.getClient()).thenReturn(client);
        when(client.getCompanyName()).thenReturn("Acme Studio");
        when(workRequest.getTitle()).thenReturn("Website refresh");
        when(workRequest.getDescription()).thenReturn("Refresh the client website");
        when(workRequest.getStatus()).thenReturn(WorkRequestStatus.IN_PROGRESS);
        when(workRequest.getPriority()).thenReturn(WorkRequestPriority.HIGH);
        when(workRequest.getRequestedBy()).thenReturn("Maya Chen");
        when(commentRepository.findByWorkRequest_IdOrderByCreatedAtDesc(eq(workRequestId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(secondComment, firstComment)));

        AiAssistantService service = new AiAssistantService(
                workRequestRepository,
                commentRepository,
                RestClient.builder(),
                objectMapper,
                accessService,
                true,
                "test-key",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-model",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                maxResponseBytes,
                maxContextComments,
                maxContextCharacters,
                maxOutputCharacters,
                maxOutputTokens
        );
        return new TestFixture(service, workRequestId, workRequestRepository, commentRepository);
    }

    private Comment comment(String author, String body) {
        Comment comment = mock(Comment.class);
        when(comment.getAuthorName()).thenReturn(author);
        when(comment.getBody()).thenReturn(body);
        return comment;
    }

    private void respond(HttpExchange exchange) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] responseBytes = providerResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private record TestFixture(
            AiAssistantService service,
            UUID workRequestId,
            WorkRequestRepository workRequestRepository,
            CommentRepository commentRepository
    ) {
    }
}
