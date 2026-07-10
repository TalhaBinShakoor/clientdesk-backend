package com.clientdesk.workrequest;

import com.clientdesk.client.Client;
import com.clientdesk.client.ClientRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class WorkRequestService {

    private final WorkRequestRepository workRequestRepository;
    private final ClientRepository clientRepository;

    public WorkRequestService(WorkRequestRepository workRequestRepository, ClientRepository clientRepository) {
        this.workRequestRepository = workRequestRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkRequestResponse> findAll(
            WorkRequestStatus status,
            WorkRequestPriority priority,
            UUID clientId
    ) {
        return workRequestRepository.findAll(matchingFilters(status, priority, clientId))
                .stream()
                .map(WorkRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkRequestResponse findById(UUID id) {
        return WorkRequestResponse.from(findWorkRequest(id));
    }

    public WorkRequestResponse create(WorkRequestCreateRequest request) {
        Client client = findClient(request.clientId());
        WorkRequest workRequest = new WorkRequest(
                client,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.requestedBy(),
                request.dueDate()
        );

        return WorkRequestResponse.from(workRequestRepository.save(workRequest));
    }

    public WorkRequestResponse update(UUID id, WorkRequestUpdateRequest request) {
        WorkRequest workRequest = findWorkRequest(id);
        Client client = findClient(request.clientId());

        workRequest.setClient(client);
        workRequest.setTitle(request.title());
        workRequest.setDescription(request.description());
        workRequest.setStatus(request.status());
        workRequest.setPriority(request.priority());
        workRequest.setRequestedBy(request.requestedBy());
        workRequest.setDueDate(request.dueDate());
        workRequest.markUpdated();

        return WorkRequestResponse.from(workRequest);
    }

    public WorkRequestResponse updateStatus(UUID id, WorkRequestStatusUpdateRequest request) {
        WorkRequest workRequest = findWorkRequest(id);
        workRequest.setStatus(request.status());
        workRequest.markUpdated();

        return WorkRequestResponse.from(workRequest);
    }

    public void delete(UUID id) {
        WorkRequest workRequest = findWorkRequest(id);
        workRequestRepository.delete(workRequest);
    }

    private Specification<WorkRequest> matchingFilters(
            WorkRequestStatus status,
            WorkRequestPriority priority,
            UUID clientId
    ) {
        return Specification.allOf(
                status == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status),
                priority == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority),
                clientId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("client").get("id"), clientId)
        );
    }

    private Client findClient(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Client not found"));
    }

    private WorkRequest findWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
    }
}
