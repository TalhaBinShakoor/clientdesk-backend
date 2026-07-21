package com.clientdesk.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiRequestRateLimiter {

    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final long windowSeconds;
    private final int maxTrackedKeys;
    private final Map<ApiRateLimitCategory, RequestLimits> limits;
    private final Clock clock;

    @Autowired
    public ApiRequestRateLimiter(
            @Value("${clientdesk.api.rate-limit.enabled:true}") boolean enabled,
            @Value("${clientdesk.api.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${clientdesk.api.rate-limit.max-tracked-keys:20000}") int maxTrackedKeys,
            @Value("${clientdesk.api.rate-limit.write.max-per-user:60}") int writePerUser,
            @Value("${clientdesk.api.rate-limit.write.max-per-organization:300}") int writePerOrganization,
            @Value("${clientdesk.api.rate-limit.upload.max-per-user:10}") int uploadPerUser,
            @Value("${clientdesk.api.rate-limit.upload.max-per-organization:50}") int uploadPerOrganization,
            @Value("${clientdesk.api.rate-limit.download.max-per-user:60}") int downloadPerUser,
            @Value("${clientdesk.api.rate-limit.download.max-per-organization:300}") int downloadPerOrganization,
            @Value("${clientdesk.api.rate-limit.ai.max-per-user:20}") int aiPerUser,
            @Value("${clientdesk.api.rate-limit.ai.max-per-organization:100}") int aiPerOrganization
    ) {
        this(
                enabled,
                windowSeconds,
                maxTrackedKeys,
                Map.of(
                        ApiRateLimitCategory.WRITE, new RequestLimits(writePerUser, writePerOrganization),
                        ApiRateLimitCategory.UPLOAD, new RequestLimits(uploadPerUser, uploadPerOrganization),
                        ApiRateLimitCategory.DOWNLOAD, new RequestLimits(downloadPerUser, downloadPerOrganization),
                        ApiRateLimitCategory.AI, new RequestLimits(aiPerUser, aiPerOrganization)
                ),
                Clock.systemUTC()
        );
    }

    ApiRequestRateLimiter(
            boolean enabled,
            long windowSeconds,
            int maxTrackedKeys,
            Map<ApiRateLimitCategory, RequestLimits> limits,
            Clock clock
    ) {
        if (windowSeconds < 1 || maxTrackedKeys < 2 || limits.values().stream().anyMatch(RequestLimits::invalid)) {
            throw new IllegalArgumentException("API rate-limit settings must be positive");
        }
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxTrackedKeys = maxTrackedKeys;
        this.limits = Map.copyOf(limits);
        this.clock = clock;
    }

    ApiRateLimitDecision consume(ApiRateLimitCategory category, UUID userId, UUID organizationId) {
        if (!enabled) {
            return ApiRateLimitDecision.permit();
        }

        RequestLimits requestLimits = limits.get(category);
        Instant now = clock.instant();
        ApiRateLimitDecision userDecision = consume(
                key(category, "user", userId),
                requestLimits.maxPerUser(),
                now
        );
        if (!userDecision.allowed()) {
            return userDecision;
        }

        return consume(
                key(category, "organization", organizationId),
                requestLimits.maxPerOrganization(),
                now
        );
    }

    private ApiRateLimitDecision consume(String key, int limit, Instant now) {
        removeIfExpired(key, now);
        if (!ensureCapacity(key, now)) {
            return ApiRateLimitDecision.deny(windowSeconds);
        }

        RateLimitWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, windowSeconds)) {
                return new RateLimitWindow(now, 1);
            }
            return current.incremented();
        });

        if (window.requestCount() <= limit) {
            return ApiRateLimitDecision.permit();
        }
        return ApiRateLimitDecision.deny(window.retryAfterSeconds(now, windowSeconds));
    }

    private boolean ensureCapacity(String key, Instant now) {
        if (windows.containsKey(key) || windows.size() < maxTrackedKeys) {
            return true;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowSeconds));
        return windows.containsKey(key) || windows.size() < maxTrackedKeys;
    }

    private void removeIfExpired(String key, Instant now) {
        windows.computeIfPresent(key, (ignored, current) ->
                current.isExpired(now, windowSeconds) ? null : current);
    }

    private String key(ApiRateLimitCategory category, String scope, UUID id) {
        return category.name().toLowerCase() + ':' + scope + ':' + id;
    }

    record RequestLimits(int maxPerUser, int maxPerOrganization) {

        boolean invalid() {
            return maxPerUser < 1 || maxPerOrganization < 1;
        }
    }

    private record RateLimitWindow(Instant startedAt, int requestCount) {

        boolean isExpired(Instant now, long windowSeconds) {
            return !now.isBefore(startedAt.plusSeconds(windowSeconds));
        }

        long retryAfterSeconds(Instant now, long windowSeconds) {
            return Math.max(1, startedAt.plusSeconds(windowSeconds).getEpochSecond() - now.getEpochSecond());
        }

        RateLimitWindow incremented() {
            return new RateLimitWindow(startedAt, requestCount + 1);
        }
    }
}
