package com.clientdesk.activity;

import com.clientdesk.api.ApiPage;
import com.clientdesk.attachment.RequestAttachment;
import com.clientdesk.comment.Comment;
import com.clientdesk.projecttask.ProjectTask;
import com.clientdesk.projecttask.ProjectTaskStatus;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import com.clientdesk.workrequest.WorkRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ActivityEventService {

    private final ActivityEventRepository activityEventRepository;
    private final WorkRequestRepository workRequestRepository;
    private final AccessService accessService;

    public ActivityEventService(
            ActivityEventRepository activityEventRepository,
            WorkRequestRepository workRequestRepository,
            AccessService accessService
    ) {
        this.activityEventRepository = activityEventRepository;
        this.workRequestRepository = workRequestRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public ApiPage<ActivityEventResponse> findForWorkRequest(UUID workRequestId, int page, int size) {
        findWorkRequest(workRequestId);

        return ApiPage.from(
                activityEventRepository.findByWorkRequest_IdOrderByCreatedAtDesc(
                        workRequestId,
                        PageRequest.of(page, size)
                ),
                ActivityEventResponse::from
        );
    }

    public void recordWorkRequestCreated(WorkRequest workRequest) {
        saveEvent(
                workRequest,
                null,
                null,
                ActivityEventType.WORK_REQUEST_CREATED,
                "Request created"
        );
    }

    public void recordWorkRequestStatusChanged(
            WorkRequest workRequest,
            WorkRequestStatus previousStatus,
            WorkRequestStatus nextStatus
    ) {
        if (previousStatus == nextStatus) {
            return;
        }

        saveEvent(
                workRequest,
                null,
                null,
                ActivityEventType.WORK_REQUEST_STATUS_CHANGED,
                "Request status changed from %s to %s".formatted(previousStatus, nextStatus)
        );
    }

    public void recordProjectTaskCreated(ProjectTask projectTask) {
        saveEvent(
                projectTask.getWorkRequest(),
                projectTask,
                null,
                ActivityEventType.PROJECT_TASK_CREATED,
                "Task created: %s".formatted(projectTask.getTitle())
        );
    }

    public void recordProjectTaskStatusChanged(
            ProjectTask projectTask,
            ProjectTaskStatus previousStatus,
            ProjectTaskStatus nextStatus
    ) {
        if (previousStatus == nextStatus) {
            return;
        }

        saveEvent(
                projectTask.getWorkRequest(),
                projectTask,
                null,
                ActivityEventType.PROJECT_TASK_STATUS_CHANGED,
                "Task status changed from %s to %s: %s".formatted(previousStatus, nextStatus, projectTask.getTitle())
        );
    }

    public void recordCommentAdded(Comment comment) {
        WorkRequest workRequest = comment.getWorkRequest() == null
                ? comment.getProjectTask().getWorkRequest()
                : comment.getWorkRequest();

        saveEvent(
                workRequest,
                comment.getProjectTask(),
                comment,
                ActivityEventType.COMMENT_ADDED,
                "Comment added"
        );
    }

    public void recordFileUploaded(RequestAttachment attachment) {
        saveEvent(
                attachment.getWorkRequest(),
                null,
                null,
                ActivityEventType.FILE_UPLOADED,
                "File uploaded: %s".formatted(attachment.getOriginalFileName())
        );
    }

    private void saveEvent(
            WorkRequest workRequest,
            ProjectTask projectTask,
            Comment comment,
            ActivityEventType eventType,
            String summary
    ) {
        activityEventRepository.save(new ActivityEvent(
                workRequest,
                projectTask,
                comment,
                accessService.currentAppUser(),
                eventType,
                accessService.displayName(),
                summary
        ));
    }

    private WorkRequest findWorkRequest(UUID id) {
        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
        accessService.requireClientAccess(workRequest.getClient(), "Work request");
        return workRequest;
    }
}
