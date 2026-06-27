package com.clientdesk.client;

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

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return ClientResponse.from(findClient(id));
    }

    public ClientResponse create(ClientRequest request) {
        Client client = new Client(
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
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Client not found"));
    }
}