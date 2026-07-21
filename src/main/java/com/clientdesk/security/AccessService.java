package com.clientdesk.security;

import com.clientdesk.client.Client;
import com.clientdesk.identity.MembershipRole;
import com.clientdesk.identity.AppUser;
import com.clientdesk.identity.AppUserRepository;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AccessService {

    private final AppUserRepository appUserRepository;
    private final SecurityAuditLogger securityAuditLogger;

    public AccessService(AppUserRepository appUserRepository, SecurityAuditLogger securityAuditLogger) {
        this.appUserRepository = appUserRepository;
        this.securityAuditLogger = securityAuditLogger;
    }

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authentication required");
        }
        return user;
    }

    public UUID organizationId() {
        return currentUser().getOrganizationId();
    }

    public AppUser currentAppUser() {
        return appUserRepository.getReferenceById(currentUser().getUserId());
    }

    public String displayName() {
        return currentUser().getDisplayName();
    }

    public UUID assignedClientId() {
        AuthenticatedUser user = currentUser();
        return user.getRole() == MembershipRole.CLIENT ? user.getClientId() : null;
    }

    public void requireClientAccess(Client client, String resourceName) {
        AuthenticatedUser user = currentUser();
        boolean sameOrganization = client.getOrganization().getId().equals(user.getOrganizationId());
        boolean permittedClient = user.getRole() != MembershipRole.CLIENT
                || client.getId().equals(user.getClientId());

        if (!sameOrganization || !permittedClient) {
            securityAuditLogger.objectAuthorizationDenied(user, resourceName, client.getId());
            throw new ResponseStatusException(NOT_FOUND, resourceName + " not found");
        }
    }

    public <T> Specification<T> scopeByClientPath(String... clientPathSegments) {
        UUID organizationId = organizationId();
        UUID clientId = assignedClientId();

        return (root, query, criteriaBuilder) -> {
            Path<?> clientPath = root;
            for (String segment : clientPathSegments) {
                clientPath = clientPath.get(segment);
            }

            var organizationPredicate = criteriaBuilder.equal(
                    clientPath.get("organization").get("id"),
                    organizationId
            );
            if (clientId == null) {
                return organizationPredicate;
            }
            return criteriaBuilder.and(
                    organizationPredicate,
                    criteriaBuilder.equal(clientPath.get("id"), clientId)
            );
        };
    }
}
