package com.clientdesk.security;

import com.clientdesk.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final String ATTACHMENT_PATH = "/api/request-attachments";
    private static final String AI_PATH = "/api/ai-assistant/";

    private final ApiRequestRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final SecurityAuditLogger securityAuditLogger;

    public ApiRateLimitFilter(
            ApiRequestRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            SecurityAuditLogger securityAuditLogger
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.securityAuditLogger = securityAuditLogger;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ApiRateLimitCategory category = category(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (category == null || authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            filterChain.doFilter(request, response);
            return;
        }

        ApiRateLimitDecision decision = rateLimiter.consume(category, user.getUserId(), user.getOrganizationId());
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            if (response.getStatus() < 400) {
                securityAuditLogger.protectedRequestSucceeded(
                        category,
                        user,
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus()
                );
            }
            return;
        }

        securityAuditLogger.apiRateLimited(category, user, decision.retryAfterSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Request rate limit exceeded. Try again later",
                request.getRequestURI()
        ));
    }

    private ApiRateLimitCategory category(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (path.startsWith(AI_PATH)) {
            return ApiRateLimitCategory.AI;
        }
        if (HttpMethod.POST.matches(method) && path.equals(ATTACHMENT_PATH)) {
            return ApiRateLimitCategory.UPLOAD;
        }
        if (HttpMethod.GET.matches(method)
                && path.startsWith(ATTACHMENT_PATH + "/")
                && path.endsWith("/download")) {
            return ApiRateLimitCategory.DOWNLOAD;
        }
        if (path.startsWith("/api/")
                && !path.startsWith("/api/auth/")
                && !HttpMethod.GET.matches(method)
                && !HttpMethod.HEAD.matches(method)
                && !HttpMethod.OPTIONS.matches(method)) {
            return ApiRateLimitCategory.WRITE;
        }
        return null;
    }
}
