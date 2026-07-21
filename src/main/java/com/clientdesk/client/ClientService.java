package com.clientdesk.client;

import com.clientdesk.api.ApiPage;
import com.clientdesk.attachment.RequestAttachmentService;
import com.clientdesk.identity.Organization;
import com.clientdesk.identity.OrganizationRepository;
import com.clientdesk.security.AccessService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final OrganizationRepository organizationRepository;
    private final RequestAttachmentService requestAttachmentService;
    private final AccessService accessService;

    public ClientService(
            ClientRepository clientRepository,
            OrganizationRepository organizationRepository,
            RequestAttachmentService requestAttachmentService,
            AccessService accessService
    ) {
        this.clientRepository = clientRepository;
        this.organizationRepository = organizationRepository;
        this.requestAttachmentService = requestAttachmentService;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ApiPage<ClientResponse> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiPage.from(
                clientRepository.findAll(accessService.scopeByClientPath(), pageRequest),
                ClientResponse::from
        );
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
        requestAttachmentService.deleteFilesForClientAfterCommit(client.getId());
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
