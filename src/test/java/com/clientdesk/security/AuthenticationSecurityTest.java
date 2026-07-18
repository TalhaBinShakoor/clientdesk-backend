package com.clientdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedRouteRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@clientdesk.test", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@clientdesk.test", "incorrect-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginCreatesSessionMeReturnsIdentityAndLogoutInvalidatesSession() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@clientdesk.test", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@clientdesk.test"))
                .andExpect(jsonPath("$.displayName").value("Alex Morgan"))
                .andExpect(jsonPath("$.organizationSlug").value("clientdesk-demo"))
                .andExpect(jsonPath("$.clientId").doesNotExist())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("02020202-0202-0202-0202-020202020201"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
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
