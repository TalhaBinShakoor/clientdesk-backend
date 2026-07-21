package com.clientdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clientdesk.frontend.origin=https://app.clientdesk.test",
        "server.servlet.session.timeout=30m",
        "server.servlet.session.cookie.http-only=true",
        "server.servlet.session.cookie.secure=true",
        "server.servlet.session.cookie.same-site=lax"
})
@AutoConfigureMockMvc
class HttpSecurityHardeningTest {

    private static final String TRUSTED_ORIGIN = "https://app.clientdesk.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void csrfEndpointIssuesHardenedBrowserReadableCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("XSRF-TOKEN=")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Path=/")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Secure")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        not(containsString("HttpOnly"))
                ))
                .andExpect(result -> assertEquals(
                        "Lax",
                        result.getResponse().getCookie("XSRF-TOKEN").getAttribute("SameSite")
                ));
    }

    @Test
    void trustedOriginReceivesCredentialedCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void untrustedOriginIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/csrf")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightAdvertisesOnlyConfiguredCorsAccess() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("X-XSRF-TOKEN")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void secureResponseIncludesDefensiveHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
                .andExpect(header().string("Strict-Transport-Security", containsString("includeSubDomains")))
                .andExpect(header().string("Strict-Transport-Security", containsString("preload")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string(
                        "Permissions-Policy",
                        "camera=(), geolocation=(), microphone=(), payment=(), usb=()"
                ));
    }

    @Test
    void productionStyleSessionPropertiesAreActive() {
        assertEquals("30m", environment.getProperty("server.servlet.session.timeout"));
        assertEquals("true", environment.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("true", environment.getProperty("server.servlet.session.cookie.secure"));
        assertEquals("lax", environment.getProperty("server.servlet.session.cookie.same-site"));
    }
}
