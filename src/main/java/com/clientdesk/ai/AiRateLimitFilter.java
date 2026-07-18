package com.clientdesk.ai;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int maxRequests;
    private final long windowSeconds;
    private final Clock clock;

    @Autowired
    public AiRateLimitFilter(
            @Value("${clientdesk.ai.rate-limit.enabled:true}") boolean enabled,
            @Value("${clientdesk.ai.rate-limit.max-requests:20}") int maxRequests,
            @Value("${clientdesk.ai.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this(enabled, maxRequests, windowSeconds, Clock.systemUTC());
    }

    AiRateLimitFilter(boolean enabled, int maxRequests, long windowSeconds, Clock clock) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/ai-assistant/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || isAllowed(clientKey(request))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"message":"AI assistant rate limit exceeded. Please try again shortly."}
                """.trim());
    }

    private boolean isAllowed(String key) {
        Instant now = clock.instant();

        RateLimitWindow window = windows.compute(key, (ignored, currentWindow) -> {
            if (currentWindow == null || currentWindow.isExpired(now, windowSeconds)) {
                return new RateLimitWindow(now, 1);
            }

            return currentWindow.incremented();
        });

        return window.requestCount() <= maxRequests;
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private record RateLimitWindow(Instant startedAt, int requestCount) {

        boolean isExpired(Instant now, long windowSeconds) {
            return startedAt.plusSeconds(windowSeconds).isBefore(now);
        }

        RateLimitWindow incremented() {
            return new RateLimitWindow(startedAt, requestCount + 1);
        }
    }
}
