package com.clientdesk.activity;

import com.clientdesk.comment.Comment;
import com.clientdesk.projecttask.ProjectTask;
import com.clientdesk.projecttask.ProjectTaskStatus;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import com.clientdesk.workrequest.WorkRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class ActivityEventService {

    private static final String SYSTEM_ACTOR = "ClientDesk";

    private final ActivityEventRepository activityEventRepository;
    private final WorkRequestRepository workRequestRepository;

    public ActivityEventService(
            ActivityEventRepository activityEventRepository,
            WorkRequestRepository workRequestRepository
    ) {
        this.activityEventRepository = activityEventRepository;
        this.workRequestRepository = workRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityEventResponse> findForWorkRequest(UUID workRequestId) {
        findWorkRequest(workRequestId);

        return activityEventRepository.findByWorkRequest_IdOrderByCreatedAtDesc(workRequestId)
                .stream()
                .map(ActivityEventResponse::from)
                .toList();
    }

    public void recordWorkRequestCreated(WorkRequest workRequest, String actorName) {
        saveEvent(
                workRequest,
                null,
                null,
                ActivityEventType.WORK_REQUEST_CREATED,
                defaultActor(actorName),
                "Request created"
        );
    }

    public void recordWorkRequestStatusChanged(
            WorkRequest workRequest,
            WorkRequestStatus previousStatus,
            WorkRequestStatus nextStatus,
            String actorName
    ) {
        if (previousStatus == nextStatus) {
            return;
        }

        saveEvent(
                workRequest,
                null,
                null,
                ActivityEventType.WORK_REQUEST_STATUS_CHANGED,
                defaultActor(actorName),
                "Request status changed from %s to %s".formatted(previousStatus, nextStatus)
        );
    }

    public void recordProjectTaskCreated(ProjectTask projectTask, String actorName) {
        saveEvent(
                projectTask.getWorkRequest(),
                projectTask,
                null,
                ActivityEventType.PROJECT_TASK_CREATED,
                defaultActor(actorName),
                "Task created: %s".formatted(projectTask.getTitle())
        );
    }

    public void recordProjectTaskStatusChanged(
            ProjectTask projectTask,
            ProjectTaskStatus previousStatus,
            ProjectTaskStatus nextStatus,
            String actorName
    ) {
        if (previousStatus == nextStatus) {
            return;
        }

        saveEvent(
                projectTask.getWorkRequest(),
                projectTask,
                null,
                ActivityEventType.PROJECT_TASK_STATUS_CHANGED,
                defaultActor(actorName),
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
                defaultActor(comment.getAuthorName()),
                "Comment added"
        );
    }

    private void saveEvent(
            WorkRequest workRequest,
            ProjectTask projectTask,
            Comment comment,
            ActivityEventType eventType,
            String actorName,
            String summary
    ) {
        activityEventRepository.save(new ActivityEvent(
                workRequest,
                projectTask,
                comment,
                eventType,
                actorName,
                summary
        ));
    }

    private String defaultActor(String actorName) {
        return actorName == null || actorName.isBlank() ? SYSTEM_ACTOR : actorName.trim();
    }

    private WorkRequest findWorkRequest(UUID id) {
        return workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
    }
}
