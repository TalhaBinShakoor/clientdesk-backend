package com.clientdesk.projecttask;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.api.ApiPage;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
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
public class ProjectTaskService {

    private final ProjectTaskRepository projectTaskRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ActivityEventService activityEventService;
    private final AccessService accessService;

    public ProjectTaskService(
            ProjectTaskRepository projectTaskRepository,
            WorkRequestRepository workRequestRepository,
            ActivityEventService activityEventService,
            AccessService accessService
    ) {
        this.projectTaskRepository = projectTaskRepository;
        this.workRequestRepository = workRequestRepository;
        this.activityEventService = activityEventService;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ApiPage<ProjectTaskResponse> findAll(
            ProjectTaskStatus status,
            String assignee,
            UUID workRequestId,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return ApiPage.from(
                projectTaskRepository.findAll(matchingFilters(status, assignee, workRequestId), pageRequest),
                ProjectTaskResponse::from
        );
    }

    @Transactional(readOnly = true)
    public ProjectTaskResponse findById(UUID id) {
        return ProjectTaskResponse.from(findProjectTask(id));
    }

    public ProjectTaskResponse create(ProjectTaskCreateRequest request) {
        WorkRequest workRequest = findWorkRequest(request.workRequestId());
        ProjectTask projectTask = new ProjectTask(
                workRequest,
                request.title(),
                request.description(),
                request.status(),
                request.assignee(),
                request.dueDate()
        );

        ProjectTask savedProjectTask = projectTaskRepository.save(projectTask);
        activityEventService.recordProjectTaskCreated(savedProjectTask);

        return ProjectTaskResponse.from(savedProjectTask);
    }

    public ProjectTaskResponse update(UUID id, ProjectTaskUpdateRequest request) {
        ProjectTask projectTask = findProjectTask(id);
        WorkRequest workRequest = findWorkRequest(request.workRequestId());
        ProjectTaskStatus previousStatus = projectTask.getStatus();

        projectTask.setWorkRequest(workRequest);
        projectTask.setTitle(request.title());
        projectTask.setDescription(request.description());
        projectTask.setStatus(request.status());
        projectTask.setAssignee(request.assignee());
        projectTask.setDueDate(request.dueDate());
        projectTask.markUpdated();
        activityEventService.recordProjectTaskStatusChanged(
                projectTask,
                previousStatus,
                projectTask.getStatus()
        );

        return ProjectTaskResponse.from(projectTask);
    }

    public ProjectTaskResponse updateStatus(UUID id, ProjectTaskStatusUpdateRequest request) {
        ProjectTask projectTask = findProjectTask(id);
        ProjectTaskStatus previousStatus = projectTask.getStatus();
        projectTask.setStatus(request.status());
        projectTask.markUpdated();
        activityEventService.recordProjectTaskStatusChanged(
                projectTask,
                previousStatus,
                projectTask.getStatus()
        );

        return ProjectTaskResponse.from(projectTask);
    }

    public void delete(UUID id) {
        ProjectTask projectTask = findProjectTask(id);
        projectTaskRepository.delete(projectTask);
    }

    private Specification<ProjectTask> matchingFilters(
            ProjectTaskStatus status,
            String assignee,
            UUID workRequestId
    ) {
        return Specification.allOf(
                accessService.scopeByClientPath("workRequest", "client"),
                status == null ? null : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status),
                assignee == null || assignee.isBlank()
                        ? null
                        : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignee"), assignee),
                workRequestId == null
                        ? null
                        : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("workRequest").get("id"), workRequestId)
        );
    }

    private WorkRequest findWorkRequest(UUID id) {
        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
        accessService.requireClientAccess(workRequest.getClient(), "Work request");
        return workRequest;
    }

    private ProjectTask findProjectTask(UUID id) {
        ProjectTask projectTask = projectTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project task not found"));
        accessService.requireClientAccess(
                projectTask.getWorkRequest().getClient(),
                "Project task"
        );
        return projectTask;
    }
}
