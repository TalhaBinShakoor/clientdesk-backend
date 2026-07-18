package com.clientdesk.workrequest;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.client.Client;
import com.clientdesk.client.ClientRepository;
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
public class WorkRequestService {

    private final WorkRequestRepository workRequestRepository;
    private final ClientRepository clientRepository;
    private final ActivityEventService activityEventService;
    private final AccessService accessService;

    public WorkRequestService(
            WorkRequestRepository workRequestRepository,
            ClientRepository clientRepository,
            ActivityEventService activityEventService,
            AccessService accessService
    ) {
        this.workRequestRepository = workRequestRepository;
        this.clientRepository = clientRepository;
        this.activityEventService = activityEventService;
        this.accessService = accessService;
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
                accessService.currentAppUser(),
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                accessService.displayName(),
                request.dueDate()
        );

        WorkRequest savedWorkRequest = workRequestRepository.save(workRequest);
        activityEventService.recordWorkRequestCreated(savedWorkRequest);

        return WorkRequestResponse.from(savedWorkRequest);
    }

    public WorkRequestResponse update(UUID id, WorkRequestUpdateRequest request) {
        WorkRequest workRequest = findWorkRequest(id);
        Client client = findClient(request.clientId());
        WorkRequestStatus previousStatus = workRequest.getStatus();

        workRequest.setClient(client);
        workRequest.setTitle(request.title());
        workRequest.setDescription(request.description());
        workRequest.setStatus(request.status());
        workRequest.setPriority(request.priority());
        workRequest.setDueDate(request.dueDate());
        workRequest.markUpdated();
        activityEventService.recordWorkRequestStatusChanged(
                workRequest,
                previousStatus,
                workRequest.getStatus()
        );

        return WorkRequestResponse.from(workRequest);
    }

    public WorkRequestResponse updateStatus(UUID id, WorkRequestStatusUpdateRequest request) {
        WorkRequest workRequest = findWorkRequest(id);
        WorkRequestStatus previousStatus = workRequest.getStatus();
        workRequest.setStatus(request.status());
        workRequest.markUpdated();
        activityEventService.recordWorkRequestStatusChanged(
                workRequest,
                previousStatus,
                workRequest.getStatus()
        );

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
                accessService.scopeByClientPath("client"),
                status == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status),
                priority == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority),
                clientId == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("client").get("id"), clientId)
        );
    }

    private Client findClient(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Client not found"));
        accessService.requireClientAccess(client, "Client");
        return client;
    }

    private WorkRequest findWorkRequest(UUID id) {
        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
        accessService.requireClientAccess(workRequest.getClient(), "Work request");
        return workRequest;
    }
}
