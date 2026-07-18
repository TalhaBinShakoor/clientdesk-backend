package com.clientdesk.client;

import com.clientdesk.identity.Organization;
import com.clientdesk.identity.OrganizationRepository;
import com.clientdesk.security.AccessService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final OrganizationRepository organizationRepository;
    private final AccessService accessService;

    public ClientService(
            ClientRepository clientRepository,
            OrganizationRepository organizationRepository,
            AccessService accessService
    ) {
        this.clientRepository = clientRepository;
        this.organizationRepository = organizationRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll(accessService.scopeByClientPath())
                .stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return ClientResponse.from(findClient(id));
    }

    public ClientResponse create(ClientRequest request) {
        Organization organization = organizationRepository.findById(accessService.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));
        Client client = new Client(
                organization,
                request.companyName(),
                request.contactName(),
                request.email(),
                request.phone(),
                request.status(),
                request.notes()
        );

        return ClientResponse.from(clientRepository.save(client));
    }

    public ClientResponse update(UUID id, ClientRequest request) {
        Client client = findClient(id);

        client.setCompanyName(request.companyName());
        client.setContactName(request.contactName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setStatus(request.status());
        client.setNotes(request.notes());
        client.markUpdated();

        return ClientResponse.from(client);
    }

    public void delete(UUID id) {
        Client client = findClient(id);
        clientRepository.delete(client);
    }

    private Client findClient(UUID id) {
        Specification<Client> matchingId = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), id);
        return clientRepository.findOne(Specification.allOf(
                        accessService.scopeByClientPath(),
                        matchingId
                ))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Client not found"));
    }
}
