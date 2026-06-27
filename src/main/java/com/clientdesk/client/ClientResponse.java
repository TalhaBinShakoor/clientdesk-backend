package com.clientdesk.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String companyName,
        String contactName,
        String email,
        String phone,
        ClientStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getCompanyName(),
                client.getContactName(),
                client.getEmail(),
                client.getPhone(),
                client.getStatus(),
                client.getNotes(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}