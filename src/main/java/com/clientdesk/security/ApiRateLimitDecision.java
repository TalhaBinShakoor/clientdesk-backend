package com.clientdesk.security;

record ApiRateLimitDecision(boolean allowed, long retryAfterSeconds) {

    static ApiRateLimitDecision permit() {
        return new ApiRateLimitDecision(true, 0);
    }

    static ApiRateLimitDecision deny(long retryAfterSeconds) {
        return new ApiRateLimitDecision(false, Math.max(1, retryAfterSeconds));
    }
}
