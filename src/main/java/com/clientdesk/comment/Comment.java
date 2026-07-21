package com.clientdesk.comment;

import com.clientdesk.identity.AppUser;
import com.clientdesk.projecttask.ProjectTask;
import com.clientdesk.workrequest.WorkRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_request_id")
    private WorkRequest workRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_task_id")
    private ProjectTask projectTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private AppUser authorUser;

    @NotBlank
    @Size(max = 200)
    @Column(name = "author_name", nullable = false, length = 200)
    private String authorName;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Comment() {
    }

    public Comment(
            WorkRequest workRequest,
            ProjectTask projectTask,
            AppUser authorUser,
            String authorName,
            String body
    ) {
        this.workRequest = workRequest;
        this.projectTask = projectTask;
        this.authorUser = authorUser;
        this.authorName = authorName;
        this.body = body;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
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

    public AppUser getAuthorUser() {
        return authorUser;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
