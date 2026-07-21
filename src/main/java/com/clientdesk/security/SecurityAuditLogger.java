package com.clientdesk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Component
public class SecurityAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditLogger.class);
    private static final int REFERENCE_HEX_LENGTH = 16;

    private final byte[] referenceSalt;

    public SecurityAuditLogger() {
        referenceSalt = new byte[32];
        new SecureRandom().nextBytes(referenceSalt);
    }

    SecurityAuditLogger(byte[] referenceSalt) {
        this.referenceSalt = referenceSalt.clone();
    }

    public void loginSucceeded(AuthenticatedUser user, String clientAddress) {
        LOGGER.info(
                "event=auth_login_succeeded user_id={} organization_id={} client_ref={}",
                user.getUserId(), user.getOrganizationId(), reference(clientAddress)
        );
    }

    public void loginFailed(String email, String clientAddress) {
        LOGGER.warn(
                "event=auth_login_failed account_ref={} client_ref={}",
                reference(normalizedEmail(email)), reference(clientAddress)
        );
    }

    public void loginRateLimited(String email, String clientAddress, long retryAfterSeconds) {
        LOGGER.warn(
                "event=auth_login_rate_limited account_ref={} client_ref={} retry_after_seconds={}",
                reference(normalizedEmail(email)), reference(clientAddress), retryAfterSeconds
        );
    }

    public void logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            LOGGER.info(
                    "event=auth_logout user_id={} organization_id={}",
                    user.getUserId(), user.getOrganizationId()
            );
        }
    }

    public void authenticationRequired(String method, String route) {
        LOGGER.warn("event=authentication_required method={} route={}", method, route(route));
    }

    public void routeAuthorizationDenied(Authentication authentication, String method, String route) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            LOGGER.warn(
                    "event=authorization_denied scope=route user_id={} organization_id={} method={} route={}",
                    user.getUserId(), user.getOrganizationId(), method, route(route)
            );
            return;
        }
        authenticationRequired(method, route);
    }

    public void objectAuthorizationDenied(
            AuthenticatedUser user,
            String resourceType,
            UUID targetClientId
    ) {
        LOGGER.warn(
                "event=authorization_denied scope=object user_id={} organization_id={} resource_type={} target_client_id={}",
                user.getUserId(), user.getOrganizationId(), token(resourceType), targetClientId
        );
    }

    public void apiRateLimited(
            ApiRateLimitCategory category,
            AuthenticatedUser user,
            long retryAfterSeconds
    ) {
        LOGGER.warn(
                "event=api_rate_limited category={} user_id={} organization_id={} retry_after_seconds={}",
                category.name().toLowerCase(Locale.ROOT),
                user.getUserId(),
                user.getOrganizationId(),
                retryAfterSeconds
        );
    }

    public void protectedRequestSucceeded(
            ApiRateLimitCategory category,
            AuthenticatedUser user,
            String method,
            String route,
            int status
    ) {
        LOGGER.info(
                "event=protected_request_succeeded category={} user_id={} organization_id={} method={} route={} status={}",
                category.name().toLowerCase(Locale.ROOT),
                user.getUserId(),
                user.getOrganizationId(),
                method,
                route(route),
                status
        );
    }

    public void apiFailure(int status, String method, String route, Class<?> exceptionType) {
        LOGGER.error(
                "event=api_failure status={} method={} route={} exception_type={}",
                status, method, route(route), exceptionType.getSimpleName()
        );
    }

    String reference(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(referenceSalt);
            digest.update((value == null ? "unknown" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest()).substring(0, REFERENCE_HEX_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private String normalizedEmail(String email) {
        return email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String route(String route) {
        if (route == null || route.isBlank()) {
            return "unknown";
        }
        String redacted = route.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "{id}"
        );
        redacted = redacted.replaceAll("[\\r\\n\\t ]", "_");
        return redacted.length() <= 200 ? redacted : redacted.substring(0, 200);
    }

    private String token(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
