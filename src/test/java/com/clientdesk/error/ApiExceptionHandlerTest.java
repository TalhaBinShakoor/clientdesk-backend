package com.clientdesk.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiExceptionHandlerTest.ErrorEndpointConfiguration.class)
@WithMockUser(roles = "ADMIN")
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    private static final String SENSITIVE_DETAIL = "database password=do-not-expose";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureDoesNotExposeRejectedValuesOrFieldDetails() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/clients"))
                .andExpect(content().string(not(containsString("companyName"))))
                .andExpect(content().string(not(containsString("rejected value"))));
    }

    @Test
    void malformedJsonDoesNotExposeParserDetails() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request is malformed"))
                .andExpect(content().string(not(containsString("JsonEOFException"))))
                .andExpect(content().string(not(containsString("HttpMessageNotReadableException"))));
    }

    @Test
    void oversizedJsonReturnsPayloadTooLarge() throws Exception {
        String oversizedJson = "{\"notes\":\"" + "x".repeat(262144) + "\"}";

        mockMvc.perform(post("/api/clients")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedJson))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message").value("JSON request body is too large"));
    }

    @Test
    void safeClientErrorReasonIsPreserved() throws Exception {
        mockMvc.perform(get("/api/clients/error-test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Client not found"))
                .andExpect(jsonPath("$.path").value("/api/clients/error-test/not-found"));
    }

    @Test
    void unexpectedFailureReturnsGenericMessageWithoutInternalDetails(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/clients/error-test/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString(SENSITIVE_DETAIL))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("stackTrace"))));

        org.assertj.core.api.Assertions.assertThat(output.getOut()).doesNotContain(SENSITIVE_DETAIL);
        org.assertj.core.api.Assertions.assertThat(output.getErr()).doesNotContain(SENSITIVE_DETAIL);
    }

    @TestConfiguration
    static class ErrorEndpointConfiguration {

        @Bean
        ErrorEndpoint errorEndpoint() {
            return new ErrorEndpoint();
        }
    }

    @RestController
    static class ErrorEndpoint {

        @GetMapping("/api/clients/error-test/not-found")
        void notFound() {
            throw new ResponseStatusException(NOT_FOUND, "Client not found");
        }

        @GetMapping("/api/clients/error-test/internal")
        void internalError() {
            throw new IllegalStateException(SENSITIVE_DETAIL);
        }
    }
}
