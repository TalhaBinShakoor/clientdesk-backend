package com.clientdesk.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionEnvironmentGuardTest {

    @Test
    void acceptsHardenedProductionSettings() {
        assertDoesNotThrow(() -> ProductionEnvironmentGuard.validate(validSettings()));
    }

    @Test
    void rejectsInvalidDatabaseSettings() {
        assertRejected(settings("http://database", "clientdesk", "secret", "https://app.example", true,
                Duration.ofMinutes(30), "native", false, ""));
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "", "secret", "https://app.example", true,
                Duration.ofMinutes(30), "native", false, ""));
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "clientdesk", "", "https://app.example",
                true, Duration.ofMinutes(30), "native", false, ""));
    }

    @Test
    void rejectsUnsafeFrontendOrigins() {
        assertRejected(settingsWithOrigin("http://app.example"));
        assertRejected(settingsWithOrigin("https://app.example/path"));
        assertRejected(settingsWithOrigin("not a URI"));
    }

    @Test
    void rejectsUnsafeSessionAndProxySettings() {
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "clientdesk", "secret",
                "https://app.example", false, Duration.ofMinutes(30), "native", false, ""));
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "clientdesk", "secret",
                "https://app.example", true, Duration.ofMinutes(31), "native", false, ""));
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "clientdesk", "secret",
                "https://app.example", true, Duration.ofMinutes(30), "none", false, ""));
    }

    @Test
    void rejectsMissingOrUnconstrainedTrustedProxyPatterns() {
        assertRejected(settingsWithProxyPattern(""));
        assertRejected(settingsWithProxyPattern(".*"));
        assertRejected(settingsWithProxyPattern("[invalid"));
    }

    @Test
    void rejectsAiWithoutApiKey() {
        assertRejected(settings("jdbc:postgresql://database/clientdesk", "clientdesk", "secret",
                "https://app.example", true, Duration.ofMinutes(30), "native", true, ""));
    }

    @Test
    void rejectsUnsafeAiProviderUrlWhenAiIsEnabled() {
        assertRejected(settingsWithAiBaseUrl("http://api.openai.com/v1"));
        assertRejected(settingsWithAiBaseUrl("https://user:secret@api.openai.com/v1"));
        assertRejected(settingsWithAiBaseUrl("https://api.openai.com/v1?key=secret"));
    }

    private ProductionEnvironmentGuard.ProductionSettings validSettings() {
        return settings(
                "jdbc:postgresql://database/clientdesk",
                "clientdesk",
                "secret",
                "https://app.example",
                true,
                Duration.ofMinutes(30),
                "native",
                false,
                ""
        );
    }

    private ProductionEnvironmentGuard.ProductionSettings settingsWithOrigin(String origin) {
        return settings(
                "jdbc:postgresql://database/clientdesk",
                "clientdesk",
                "secret",
                origin,
                true,
                Duration.ofMinutes(30),
                "native",
                false,
                ""
        );
    }

    private ProductionEnvironmentGuard.ProductionSettings settings(
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword,
            String frontendOrigin,
            boolean secureSessionCookie,
            Duration sessionTimeout,
            String forwardHeadersStrategy,
            boolean aiEnabled,
            String openAiApiKey
    ) {
        return new ProductionEnvironmentGuard.ProductionSettings(
                datasourceUrl,
                datasourceUsername,
                datasourcePassword,
                frontendOrigin,
                secureSessionCookie,
                sessionTimeout,
                forwardHeadersStrategy,
                "10\\.0\\.0\\.[0-9]+",
                aiEnabled,
                "https://api.openai.com/v1",
                openAiApiKey
        );
    }

    private ProductionEnvironmentGuard.ProductionSettings settingsWithProxyPattern(String proxyPattern) {
        return new ProductionEnvironmentGuard.ProductionSettings(
                "jdbc:postgresql://database/clientdesk",
                "clientdesk",
                "secret",
                "https://app.example",
                true,
                Duration.ofMinutes(30),
                "native",
                proxyPattern,
                false,
                "https://api.openai.com/v1",
                ""
        );
    }

    private ProductionEnvironmentGuard.ProductionSettings settingsWithAiBaseUrl(String baseUrl) {
        return new ProductionEnvironmentGuard.ProductionSettings(
                "jdbc:postgresql://database/clientdesk",
                "clientdesk",
                "secret",
                "https://app.example",
                true,
                Duration.ofMinutes(30),
                "native",
                "10\\.0\\.0\\.[0-9]+",
                true,
                baseUrl,
                "api-key"
        );
    }

    private void assertRejected(ProductionEnvironmentGuard.ProductionSettings settings) {
        assertThrows(IllegalStateException.class, () -> ProductionEnvironmentGuard.validate(settings));
    }
}
