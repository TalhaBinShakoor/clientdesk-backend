package com.clientdesk.security;

import com.clientdesk.identity.AppUserRepository;
import com.clientdesk.identity.OrganizationRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Profile("prod & !demo")
public class ProductionDemoDataGuard implements InitializingBean {

    private static final UUID DEMO_ORGANIZATION_ID =
            UUID.fromString("01010101-0101-0101-0101-010101010101");
    private static final List<String> DEMO_EMAILS = List.of(
            "admin@clientdesk.test",
            "team@clientdesk.test",
            "client@clientdesk.test"
    );

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;

    public ProductionDemoDataGuard(
            OrganizationRepository organizationRepository,
            AppUserRepository appUserRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void afterPropertiesSet() {
        boolean demoOrganizationExists = organizationRepository.existsById(DEMO_ORGANIZATION_ID);
        boolean demoUserExists = DEMO_EMAILS.stream()
                .anyMatch(email -> appUserRepository.findByEmailIgnoreCase(email).isPresent());

        rejectDemoData(demoOrganizationExists, demoUserExists);
    }

    static void rejectDemoData(boolean demoOrganizationExists, boolean demoUserExists) {
        if (demoOrganizationExists || demoUserExists) {
            throw new IllegalStateException(
                    "Production startup refused because demo data is present. "
                            + "Use a clean production database or activate the demo profile intentionally."
            );
        }
    }
}
