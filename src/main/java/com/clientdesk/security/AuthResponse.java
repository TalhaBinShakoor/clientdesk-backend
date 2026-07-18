package com.clientdesk.security;

import com.clientdesk.identity.MembershipRole;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String displayName,
        UUID organizationId,
        UUID clientId,
        String organizationName,
        String organizationSlug,
        MembershipRole role
) {
    static AuthResponse from(AuthenticatedUser user) {
        return new AuthResponse(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getOrganizationId(),
                user.getClientId(),
                user.getOrganizationName(),
                user.getOrganizationSlug(),
                user.getRole()
        );
    }
}
