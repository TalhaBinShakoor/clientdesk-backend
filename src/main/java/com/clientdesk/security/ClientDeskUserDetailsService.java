package com.clientdesk.security;

import com.clientdesk.identity.AppUser;
import com.clientdesk.identity.AppUserRepository;
import com.clientdesk.identity.OrganizationMembership;
import com.clientdesk.identity.OrganizationMembershipRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ClientDeskUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public ClientDeskUserDetailsService(
            AppUserRepository appUserRepository,
            OrganizationMembershipRepository membershipRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        AppUser user = appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        List<OrganizationMembership> memberships =
                membershipRepository.findAllByUser_IdOrderByCreatedAtAsc(user.getId());
        if (memberships.isEmpty()) {
            throw new UsernameNotFoundException("Invalid email or password");
        }

        return AuthenticatedUser.from(memberships.getFirst());
    }
}
