package com.clientdesk.comment;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.projecttask.ProjectTask;
import com.clientdesk.projecttask.ProjectTaskRepository;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ActivityEventService activityEventService;
    private final AccessService accessService;

    public CommentService(
            CommentRepository commentRepository,
            WorkRequestRepository workRequestRepository,
            ProjectTaskRepository projectTaskRepository,
            ActivityEventService activityEventService,
            AccessService accessService
    ) {
        this.commentRepository = commentRepository;
        this.workRequestRepository = workRequestRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.activityEventService = activityEventService;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findAll(UUID workRequestId, UUID projectTaskId) {
        validateSingleTarget(workRequestId, projectTaskId);

        if (workRequestId != null) {
            findWorkRequest(workRequestId);
            return commentRepository.findByWorkRequest_IdOrderByCreatedAtAsc(workRequestId)
                    .stream()
                    .map(CommentResponse::from)
                    .toList();
        }

        findProjectTask(projectTaskId);
        return commentRepository.findByProjectTask_IdOrderByCreatedAtAsc(projectTaskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    public CommentResponse create(CommentCreateRequest request) {
        validateSingleTarget(request.workRequestId(), request.projectTaskId());

        WorkRequest workRequest = null;
        ProjectTask projectTask = null;

        if (request.workRequestId() != null) {
            workRequest = findWorkRequest(request.workRequestId());
        } else {
            projectTask = findProjectTask(request.projectTaskId());
        }

        Comment comment = new Comment(
                workRequest,
                projectTask,
                accessService.currentAppUser(),
                accessService.displayName(),
                request.body().trim()
        );
        Comment savedComment = commentRepository.save(comment);
        activityEventService.recordCommentAdded(savedComment);

        return CommentResponse.from(savedComment);
    }

    private void validateSingleTarget(UUID workRequestId, UUID projectTaskId) {
        if ((workRequestId == null && projectTaskId == null) || (workRequestId != null && projectTaskId != null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Exactly one comment target is required");
        }
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
