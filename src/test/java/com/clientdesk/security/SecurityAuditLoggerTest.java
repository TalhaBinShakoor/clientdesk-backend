package com.clientdesk.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditLoggerTest {

    private static final String EMAIL = "private.user@example.com";
    private static final String CLIENT_ADDRESS = "203.0.113.42";
    private static final String SENSITIVE_EXCEPTION_MESSAGE = "password=never-log-this";

    private final Logger logger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private SecurityAuditLogger auditLogger;

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
        auditLogger = new SecurityAuditLogger("test-salt".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void pseudonymizesAccountAndClientAddress() {
        auditLogger.loginFailed(EMAIL, CLIENT_ADDRESS);

        String output = output();
        assertThat(output).contains("event=auth_login_failed", "account_ref=", "client_ref=");
        assertThat(output).doesNotContain(EMAIL, CLIENT_ADDRESS, "private.user");
    }

    @Test
    void redactsIdentifiersFromRoutesAndOmitsExceptionMessages() {
        UUID resourceId = UUID.randomUUID();
        auditLogger.authenticationRequired("GET", "/api/clients/" + resourceId);
        auditLogger.apiFailure(500, "GET", "/api/clients/" + resourceId,
                new IllegalStateException(SENSITIVE_EXCEPTION_MESSAGE).getClass());

        String output = output();
        assertThat(output).contains("route=/api/clients/{id}", "exception_type=IllegalStateException");
        assertThat(output).doesNotContain(resourceId.toString(), SENSITIVE_EXCEPTION_MESSAGE);
    }

    private String output() {
        return String.join("\n", appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList());
    }
}
