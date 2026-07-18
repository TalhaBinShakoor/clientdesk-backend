package com.clientdesk.security;

import com.clientdesk.identity.MembershipRole;
import com.clientdesk.identity.OrganizationMembership;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UUID organizationId;
    private final UUID clientId;
    private final String organizationName;
    private final String organizationSlug;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final MembershipRole role;
    private final boolean enabled;

    private AuthenticatedUser(
            UUID userId,
            UUID organizationId,
            UUID clientId,
            String organizationName,
            String organizationSlug,
            String email,
            String displayName,
            String passwordHash,
            MembershipRole role,
            boolean enabled
    ) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.organizationName = organizationName;
        this.organizationSlug = organizationSlug;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    static AuthenticatedUser from(OrganizationMembership membership) {
        return new AuthenticatedUser(
                membership.getUser().getId(),
                membership.getOrganization().getId(),
                membership.getClient() == null ? null : membership.getClient().getId(),
                membership.getOrganization().getName(),
                membership.getOrganization().getSlug(),
                membership.getUser().getEmail(),
                membership.getUser().getDisplayName(),
                membership.getUser().getPasswordHash(),
                membership.getRole(),
                membership.getUser().isEnabled()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MembershipRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
