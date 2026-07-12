package com.clientdesk.projecttask;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ProjectTaskService {

    private final ProjectTaskRepository projectTaskRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ActivityEventService activityEventService;

    public ProjectTaskService(
            ProjectTaskRepository projectTaskRepository,
            WorkRequestRepository workRequestRepository,
            ActivityEventService activityEventService
    ) {
        this.projectTaskRepository = projectTaskRepository;
        this.workRequestRepository = workRequestRepository;
        this.activityEventService = activityEventService;
    }

    @Transactional(readOnly = true)
    public List<ProjectTaskResponse> findAll(
            ProjectTaskStatus status,
            String assignee,
            UUID workRequestId
    ) {
        return projectTaskRepository.findAll(matchingFilters(status, assignee, workRequestId))
                .stream()
                .map(ProjectTaskResponse::from)
                .toList();
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
        activityEventService.recordProjectTaskCreated(savedProjectTask, null);

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
                projectTask.getStatus(),
                null
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
                projectTask.getStatus(),
                null
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
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
    }

    private ProjectTask findProjectTask(UUID id) {
        return projectTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Project task not found"));
    }
}
