package com.clientdesk.workrequest;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.api.ApiPage;
import com.clientdesk.attachment.RequestAttachmentService;
import com.clientdesk.client.Client;
import com.clientdesk.client.ClientRepository;
import com.clientdesk.identity.MembershipRole;
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
public class WorkRequestService {

    private final WorkRequestRepository workRequestRepository;
    private final ClientRepository clientRepository;
    private final ActivityEventService activityEventService;
    private final RequestAttachmentService requestAttachmentService;
    private final AccessService accessService;

    public WorkRequestService(
            WorkRequestRepository workRequestRepository,
            ClientRepository clientRepository,
            ActivityEventService activityEventService,
            RequestAttachmentService requestAttachmentService,
            AccessService accessService
    ) {
        this.workRequestRepository = workRequestRepository;
        this.clientRepository = clientRepository;
        this.activityEventService = activityEventService;
        this.requestAttachmentService = requestAttachmentService;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ApiPage<WorkRequestResponse> findAll(
            WorkRequestStatus status,
            WorkRequestPriority priority,
            UUID clientId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiPage.from(
                workRequestRepository.findAll(matchingFilters(status, priority, clientId), pageRequest),
                WorkRequestResponse::from
        );
    }

    @Transactional(readOnly = true)
    public WorkRequestResponse findById(UUID id) {
        return WorkRequestResponse.from(findWorkRequest(id));
    }

    public WorkRequestResponse create(WorkRequestCreateRequest request) {
        Client client = findClient(request.clientId());
        WorkRequestStatus initialStatus = accessService.currentUser().getRole() == MembershipRole.CLIENT
                ? WorkRequestStatus.NEW
                : request.status();
        WorkRequest workRequest = new WorkRequest(
                client,
                accessService.currentAppUser(),
                request.title(),
                request.description(),
                initialStatus,
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
        requestAttachmentService.deleteFilesForWorkRequestAfterCommit(workRequest.getId());
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
