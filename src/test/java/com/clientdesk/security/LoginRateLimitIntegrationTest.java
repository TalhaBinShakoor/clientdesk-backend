package com.clientdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clientdesk.auth.rate-limit.enabled=true",
        "clientdesk.auth.rate-limit.max-failures-per-account-ip=2",
        "clientdesk.auth.rate-limit.max-failures-per-ip=10",
        "clientdesk.auth.rate-limit.window-seconds=60",
        "clientdesk.auth.rate-limit.max-tracked-keys=100"
})
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void repeatedFailuresReturnTooManyRequestsWithRetryAfter() throws Exception {
        attempt("blocked@example.com", "incorrect", "192.0.2.10", null)
                .andExpect(status().isUnauthorized());
        attempt("blocked@example.com", "incorrect", "192.0.2.10", null)
                .andExpect(status().isUnauthorized());

        attempt("blocked@example.com", "incorrect", "192.0.2.10", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9][0-9]?")))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Try again later"));
    }

    @Test
    void emailMatchingIsCaseInsensitive() throws Exception {
        attempt("CASE@example.com", "incorrect", "192.0.2.11", null)
                .andExpect(status().isUnauthorized());
        attempt("case@example.com", "incorrect", "192.0.2.11", null)
                .andExpect(status().isUnauthorized());
        attempt("Case@example.com", "incorrect", "192.0.2.11", null)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void successfulLoginResetsAccountIpFailures() throws Exception {
        attempt("admin@clientdesk.test", "incorrect", "192.0.2.12", null)
                .andExpect(status().isUnauthorized());
        attempt("admin@clientdesk.test", "password", "192.0.2.12", null)
                .andExpect(status().isOk());

        attempt("admin@clientdesk.test", "incorrect", "192.0.2.12", null)
                .andExpect(status().isUnauthorized());
        attempt("admin@clientdesk.test", "incorrect", "192.0.2.12", null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void spoofedForwardedForDoesNotBypassRemoteAddressLimit() throws Exception {
        attempt("spoof@example.com", "incorrect", "192.0.2.13", "198.51.100.1")
                .andExpect(status().isUnauthorized());
        attempt("spoof@example.com", "incorrect", "192.0.2.13", "198.51.100.2")
                .andExpect(status().isUnauthorized());
        attempt("spoof@example.com", "incorrect", "192.0.2.13", "198.51.100.3")
                .andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions attempt(
            String email,
            String password,
            String remoteAddress,
            String forwardedFor
    ) throws Exception {
        var request = post("/api/auth/login")
                .with(csrf())
                .with(servletRequest -> {
                    servletRequest.setRemoteAddr(remoteAddress);
                    return servletRequest;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password));
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return mockMvc.perform(request);
    }

    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
