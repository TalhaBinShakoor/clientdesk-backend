package com.clientdesk.workrequest;

import com.clientdesk.activity.ActivityEventService;
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
    private final ActivityEventService activityEventService;

    public WorkRequestService(
            WorkRequestRepository workRequestRepository,
            ClientRepository clientRepository,
            ActivityEventService activityEventService
    ) {
        this.workRequestRepository = workRequestRepository;
        this.clientRepository = clientRepository;
        this.activityEventService = activityEventService;
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

        WorkRequest savedWorkRequest = workRequestRepository.save(workRequest);
        activityEventService.recordWorkRequestCreated(savedWorkRequest, request.requestedBy());

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
        workRequest.setRequestedBy(request.requestedBy());
        workRequest.setDueDate(request.dueDate());
        workRequest.markUpdated();
        activityEventService.recordWorkRequestStatusChanged(
                workRequest,
                previousStatus,
                workRequest.getStatus(),
                request.requestedBy()
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
                workRequest.getStatus(),
                null
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
