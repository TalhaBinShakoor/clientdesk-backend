package com.clientdesk.activity;

import com.clientdesk.identity.AppUser;
import com.clientdesk.comment.Comment;
import com.clientdesk.projecttask.ProjectTask;
import com.clientdesk.workrequest.WorkRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_task_id")
    private ProjectTask projectTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    @Size(max = 200)
    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActivityEvent() {
    }

    public ActivityEvent(
            WorkRequest workRequest,
            ProjectTask projectTask,
            Comment comment,
            AppUser actorUser,
            ActivityEventType eventType,
            String actorName,
            String summary
    ) {
        this.workRequest = workRequest;
        this.projectTask = projectTask;
        this.comment = comment;
        this.actorUser = actorUser;
        this.eventType = eventType;
        this.actorName = actorName;
        this.summary = summary;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public ProjectTask getProjectTask() {
        return projectTask;
    }

    public Comment getComment() {
        return comment;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public String getActorName() {
        return actorName;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
