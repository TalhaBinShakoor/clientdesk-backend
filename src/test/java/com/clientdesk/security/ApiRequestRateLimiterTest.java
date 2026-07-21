package com.clientdesk.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRequestRateLimiterTest {

    @Test
    void isolatesUsersAndResetsExpiredWindows() {
        MutableClock clock = new MutableClock();
        ApiRequestRateLimiter limiter = limiter(true, 60, 100, 1, 10, clock);
        UUID organizationId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        assertTrue(limiter.consume(ApiRateLimitCategory.WRITE, firstUserId, organizationId).allowed());
        assertFalse(limiter.consume(ApiRateLimitCategory.WRITE, firstUserId, organizationId).allowed());
        assertTrue(limiter.consume(ApiRateLimitCategory.WRITE, secondUserId, organizationId).allowed());

        clock.advance(Duration.ofSeconds(60));
        assertTrue(limiter.consume(ApiRateLimitCategory.WRITE, firstUserId, organizationId).allowed());
    }

    @Test
    void enforcesOrganizationAggregateAcrossUsers() {
        ApiRequestRateLimiter limiter = limiter(true, 60, 100, 10, 1, Clock.systemUTC());
        UUID organizationId = UUID.randomUUID();

        assertTrue(limiter.consume(
                ApiRateLimitCategory.UPLOAD,
                UUID.randomUUID(),
                organizationId
        ).allowed());
        assertFalse(limiter.consume(
                ApiRateLimitCategory.UPLOAD,
                UUID.randomUUID(),
                organizationId
        ).allowed());
    }

    @Test
    void failsClosedWhenTrackingCapacityIsExhausted() {
        ApiRequestRateLimiter limiter = limiter(true, 60, 2, 10, 10, Clock.systemUTC());

        assertTrue(limiter.consume(
                ApiRateLimitCategory.DOWNLOAD,
                UUID.randomUUID(),
                UUID.randomUUID()
        ).allowed());
        assertFalse(limiter.consume(
                ApiRateLimitCategory.DOWNLOAD,
                UUID.randomUUID(),
                UUID.randomUUID()
        ).allowed());
    }

    @Test
    void disabledLimiterAlwaysPermitsRequests() {
        ApiRequestRateLimiter limiter = limiter(false, 60, 2, 1, 1, Clock.systemUTC());
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        assertTrue(limiter.consume(ApiRateLimitCategory.AI, userId, organizationId).allowed());
        assertTrue(limiter.consume(ApiRateLimitCategory.AI, userId, organizationId).allowed());
    }

    private ApiRequestRateLimiter limiter(
            boolean enabled,
            long windowSeconds,
            int maxTrackedKeys,
            int maxPerUser,
            int maxPerOrganization,
            Clock clock
    ) {
        Map<ApiRateLimitCategory, ApiRequestRateLimiter.RequestLimits> limits =
                new EnumMap<>(ApiRateLimitCategory.class);
        for (ApiRateLimitCategory category : ApiRateLimitCategory.values()) {
            limits.put(category, new ApiRequestRateLimiter.RequestLimits(maxPerUser, maxPerOrganization));
        }
        return new ApiRequestRateLimiter(enabled, windowSeconds, maxTrackedKeys, limits, clock);
    }

    private static class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
