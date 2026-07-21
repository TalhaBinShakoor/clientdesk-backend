package com.clientdesk.projecttask;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_tasks")
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_request_id", nullable = false)
    private WorkRequest workRequest;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 10000)
    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectTaskStatus status = ProjectTaskStatus.TODO;

    @Size(max = 200)
    @Column(length = 200)
    private String assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectTask() {
    }

    public ProjectTask(
            WorkRequest workRequest,
            String title,
            String description,
            ProjectTaskStatus status,
            String assignee,
            LocalDate dueDate
    ) {
        this.workRequest = workRequest;
        this.title = title;
        this.description = description;
        this.status = status == null ? ProjectTaskStatus.TODO : status;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        markUpdated();
    }

    void markUpdated() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public WorkRequest getWorkRequest() {
        return workRequest;
    }

    public void setWorkRequest(WorkRequest workRequest) {
        this.workRequest = workRequest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectTaskStatus status) {
        this.status = status == null ? ProjectTaskStatus.TODO : status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
