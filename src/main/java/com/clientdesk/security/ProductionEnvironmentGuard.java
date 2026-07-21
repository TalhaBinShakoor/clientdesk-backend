package com.clientdesk.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
@Profile("prod")
public class ProductionEnvironmentGuard implements InitializingBean {

    private final ProductionSettings settings;

    public ProductionEnvironmentGuard(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String datasourceUsername,
            @Value("${spring.datasource.password}") String datasourcePassword,
            @Value("${clientdesk.frontend.origin}") String frontendOrigin,
            @Value("${server.servlet.session.cookie.secure}") boolean secureSessionCookie,
            @Value("${server.servlet.session.timeout}") Duration sessionTimeout,
            @Value("${server.forward-headers-strategy}") String forwardHeadersStrategy,
            @Value("${server.tomcat.remoteip.internal-proxies:}") String trustedProxyIpPattern,
            @Value("${clientdesk.ai.enabled:false}") boolean aiEnabled,
            @Value("${clientdesk.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            @Value("${clientdesk.ai.openai.api-key:}") String openAiApiKey
    ) {
        this.settings = new ProductionSettings(
                datasourceUrl,
                datasourceUsername,
                datasourcePassword,
                frontendOrigin,
                secureSessionCookie,
                sessionTimeout,
                forwardHeadersStrategy,
                trustedProxyIpPattern,
                aiEnabled,
                openAiBaseUrl,
                openAiApiKey
        );
    }

    @Override
    public void afterPropertiesSet() {
        validate(settings);
    }

    static void validate(ProductionSettings settings) {
        if (isBlank(settings.datasourceUrl())
                || !settings.datasourceUrl().startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Production requires a PostgreSQL JDBC datasource URL");
        }
        if (isBlank(settings.datasourceUsername()) || isBlank(settings.datasourcePassword())) {
            throw new IllegalStateException("Production database credentials must not be blank");
        }

        validateFrontendOrigin(settings.frontendOrigin());

        if (!settings.secureSessionCookie()) {
            throw new IllegalStateException("Production session cookies must be secure");
        }
        if (settings.sessionTimeout() == null
                || settings.sessionTimeout().isZero()
                || settings.sessionTimeout().isNegative()
                || settings.sessionTimeout().compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalStateException("Production session timeout must be between 1 second and 30 minutes");
        }
        if (isBlank(settings.forwardHeadersStrategy())
                || "none".equalsIgnoreCase(settings.forwardHeadersStrategy())) {
            throw new IllegalStateException("Production must process trusted proxy headers");
        }
        validateTrustedProxyPattern(settings.trustedProxyIpPattern());
        if (settings.aiEnabled()) {
            if (isBlank(settings.openAiApiKey())) {
                throw new IllegalStateException("AI cannot be enabled in production without an API key");
            }
            validateAiBaseUrl(settings.openAiBaseUrl());
        }
    }

    private static void validateFrontendOrigin(String frontendOrigin) {
        try {
            URI origin = URI.create(frontendOrigin);
            boolean invalid = !"https".equalsIgnoreCase(origin.getScheme())
                    || isBlank(origin.getHost())
                    || origin.getUserInfo() != null
                    || origin.getQuery() != null
                    || origin.getFragment() != null
                    || (origin.getPath() != null && !origin.getPath().isEmpty());
            if (invalid) {
                throw new IllegalStateException("Production frontend origin must be an HTTPS origin without a path");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Production frontend origin must be a valid HTTPS origin", exception);
        }
    }

    private static void validateTrustedProxyPattern(String trustedProxyIpPattern) {
        if (isBlank(trustedProxyIpPattern)
                || ".*".equals(trustedProxyIpPattern.trim())
                || "^.*$".equals(trustedProxyIpPattern.trim())) {
            throw new IllegalStateException("Production requires a constrained trusted proxy IP pattern");
        }
        try {
            Pattern.compile(trustedProxyIpPattern);
        } catch (PatternSyntaxException exception) {
            throw new IllegalStateException("Production trusted proxy IP pattern must be valid", exception);
        }
    }

    private static void validateAiBaseUrl(String openAiBaseUrl) {
        try {
            URI uri = URI.create(openAiBaseUrl);
            boolean invalid = !"https".equalsIgnoreCase(uri.getScheme())
                    || isBlank(uri.getHost())
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null;
            if (invalid) {
                throw new IllegalStateException("Production AI base URL must use HTTPS without credentials or parameters");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Production AI base URL must be valid", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record ProductionSettings(
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword,
            String frontendOrigin,
            boolean secureSessionCookie,
            Duration sessionTimeout,
            String forwardHeadersStrategy,
            String trustedProxyIpPattern,
            boolean aiEnabled,
            String openAiBaseUrl,
            String openAiApiKey
    ) {
    }
}
