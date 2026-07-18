package com.clientdesk.security;

public record CsrfResponse(
        String headerName,
        String token
) {
}
