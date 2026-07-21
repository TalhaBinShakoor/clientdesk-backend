package com.clientdesk.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    @Test
    void blocksAtAccountIpThresholdAndAllowsAfterExpiry() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = limiter(2, 10, 60, 100, clock);

        assertTrue(limiter.isAllowed("USER@example.com", "192.0.2.1"));
        limiter.recordFailure("USER@example.com", "192.0.2.1");
        assertTrue(limiter.isAllowed("user@example.com", "192.0.2.1"));
        limiter.recordFailure("user@example.com", "192.0.2.1");
        assertFalse(limiter.isAllowed("user@example.com", "192.0.2.1"));

        clock.advance(Duration.ofSeconds(60));
        assertTrue(limiter.isAllowed("user@example.com", "192.0.2.1"));
    }

    @Test
    void aggregatesFailuresAcrossAccountsForSameIp() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = limiter(5, 2, 60, 100, clock);

        limiter.recordFailure("first@example.com", "192.0.2.2");
        limiter.recordFailure("second@example.com", "192.0.2.2");

        assertFalse(limiter.isAllowed("third@example.com", "192.0.2.2"));
        assertTrue(limiter.isAllowed("third@example.com", "192.0.2.3"));
    }

    @Test
    void successfulLoginClearsOnlyTheAccountIpPair() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = limiter(1, 2, 60, 100, clock);

        limiter.recordFailure("user@example.com", "192.0.2.4");
        assertFalse(limiter.isAllowed("user@example.com", "192.0.2.4"));

        limiter.recordSuccess("user@example.com", "192.0.2.4");
        assertTrue(limiter.isAllowed("user@example.com", "192.0.2.4"));

        limiter.recordFailure("other@example.com", "192.0.2.4");
        assertFalse(limiter.isAllowed("new@example.com", "192.0.2.4"));
    }

    @Test
    void failsClosedWhenTrackingCapacityIsExhausted() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = limiter(5, 10, 60, 2, clock);

        assertTrue(limiter.isAllowed("first@example.com", "192.0.2.5"));
        limiter.recordFailure("first@example.com", "192.0.2.5");

        assertFalse(limiter.isAllowed("second@example.com", "192.0.2.6"));
    }

    @Test
    void disabledLimiterAlwaysAllowsRequests() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                false,
                1,
                1,
                60,
                2,
                Clock.systemUTC()
        );

        limiter.recordFailure("user@example.com", "192.0.2.7");
        assertTrue(limiter.isAllowed("user@example.com", "192.0.2.7"));
    }

    private LoginAttemptLimiter limiter(
            int maxFailuresPerAccountIp,
            int maxFailuresPerIp,
            long windowSeconds,
            int maxTrackedKeys,
            Clock clock
    ) {
        return new LoginAttemptLimiter(
                true,
                maxFailuresPerAccountIp,
                maxFailuresPerIp,
                windowSeconds,
                maxTrackedKeys,
                clock
        );
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
