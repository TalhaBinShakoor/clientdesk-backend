package com.clientdesk.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptLimiter {

    private static final String ACCOUNT_IP_PREFIX = "account-ip:";
    private static final String IP_PREFIX = "ip:";

    private final Map<String, AttemptWindow> windows = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int maxFailuresPerAccountIp;
    private final int maxFailuresPerIp;
    private final long windowSeconds;
    private final int maxTrackedKeys;
    private final Clock clock;

    @Autowired
    public LoginAttemptLimiter(
            @Value("${clientdesk.auth.rate-limit.enabled:true}") boolean enabled,
            @Value("${clientdesk.auth.rate-limit.max-failures-per-account-ip:5}") int maxFailuresPerAccountIp,
            @Value("${clientdesk.auth.rate-limit.max-failures-per-ip:20}") int maxFailuresPerIp,
            @Value("${clientdesk.auth.rate-limit.window-seconds:900}") long windowSeconds,
            @Value("${clientdesk.auth.rate-limit.max-tracked-keys:10000}") int maxTrackedKeys
    ) {
        this(
                enabled,
                maxFailuresPerAccountIp,
                maxFailuresPerIp,
                windowSeconds,
                maxTrackedKeys,
                Clock.systemUTC()
        );
    }

    LoginAttemptLimiter(
            boolean enabled,
            int maxFailuresPerAccountIp,
            int maxFailuresPerIp,
            long windowSeconds,
            int maxTrackedKeys,
            Clock clock
    ) {
        if (maxFailuresPerAccountIp < 1
                || maxFailuresPerIp < 1
                || windowSeconds < 1
                || maxTrackedKeys < 2) {
            throw new IllegalArgumentException("Login rate-limit settings must be positive");
        }
        this.enabled = enabled;
        this.maxFailuresPerAccountIp = maxFailuresPerAccountIp;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.windowSeconds = windowSeconds;
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
    }

    public boolean isAllowed(String email, String clientIp) {
        if (!enabled) {
            return true;
        }

        Instant now = clock.instant();
        String accountIpKey = accountIpKey(email, clientIp);
        String ipKey = ipKey(clientIp);
        removeIfExpired(accountIpKey, now);
        removeIfExpired(ipKey, now);

        if (!hasTrackingCapacity(accountIpKey, ipKey, now)) {
            return false;
        }

        return failureCount(accountIpKey) < maxFailuresPerAccountIp
                && failureCount(ipKey) < maxFailuresPerIp;
    }

    public void recordFailure(String email, String clientIp) {
        if (!enabled) {
            return;
        }

        Instant now = clock.instant();
        increment(accountIpKey(email, clientIp), now);
        increment(ipKey(clientIp), now);
    }

    public void recordSuccess(String email, String clientIp) {
        if (enabled) {
            windows.remove(accountIpKey(email, clientIp));
        }
    }

    public long retryAfterSeconds(String email, String clientIp) {
        Instant now = clock.instant();
        long accountRetry = retryAfter(accountIpKey(email, clientIp), now);
        long ipRetry = retryAfter(ipKey(clientIp), now);
        return Math.max(1, Math.max(accountRetry, ipRetry));
    }

    private void increment(String key, Instant now) {
        windows.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, windowSeconds)) {
                return new AttemptWindow(now, 1);
            }
            return current.incremented();
        });
    }

    private boolean hasTrackingCapacity(String accountIpKey, String ipKey, Instant now) {
        int requiredSlots = requiredSlots(accountIpKey, ipKey);
        if (windows.size() + requiredSlots <= maxTrackedKeys) {
            return true;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowSeconds));
        return windows.size() + requiredSlots(accountIpKey, ipKey) <= maxTrackedKeys;
    }

    private int requiredSlots(String accountIpKey, String ipKey) {
        int requiredSlots = windows.containsKey(accountIpKey) ? 0 : 1;
        return requiredSlots + (windows.containsKey(ipKey) ? 0 : 1);
    }

    private void removeIfExpired(String key, Instant now) {
        windows.computeIfPresent(key, (ignored, current) ->
                current.isExpired(now, windowSeconds) ? null : current);
    }

    private int failureCount(String key) {
        AttemptWindow window = windows.get(key);
        return window == null ? 0 : window.failureCount();
    }

    private long retryAfter(String key, Instant now) {
        AttemptWindow window = windows.get(key);
        if (window == null) {
            return 0;
        }
        return Math.max(0, window.startedAt().plusSeconds(windowSeconds).getEpochSecond() - now.getEpochSecond());
    }

    private String accountIpKey(String email, String clientIp) {
        return ACCOUNT_IP_PREFIX + email.trim().toLowerCase(Locale.ROOT) + ':' + clientIp;
    }

    private String ipKey(String clientIp) {
        return IP_PREFIX + clientIp;
    }

    private record AttemptWindow(Instant startedAt, int failureCount) {

        boolean isExpired(Instant now, long windowSeconds) {
            return !now.isBefore(startedAt.plusSeconds(windowSeconds));
        }

        AttemptWindow incremented() {
            return new AttemptWindow(startedAt, failureCount + 1);
        }
    }
}
