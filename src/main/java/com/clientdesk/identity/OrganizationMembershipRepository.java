package com.clientdesk.identity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    @EntityGraph(attributePaths = {"organization", "user", "client"})
    List<OrganizationMembership> findAllByUser_IdOrderByCreatedAtAsc(UUID userId);

    @EntityGraph(attributePaths = {"organization", "user", "client"})
    Optional<OrganizationMembership> findByUser_IdAndOrganization_Id(UUID userId, UUID organizationId);
}
