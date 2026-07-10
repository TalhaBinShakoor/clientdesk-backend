package com.clientdesk.workrequest;

import com.clientdesk.client.Client;
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
@Table(name = "work_requests")
public class WorkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkRequestStatus status = WorkRequestStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkRequestPriority priority = WorkRequestPriority.MEDIUM;

    @Size(max = 200)
    @Column(name = "requested_by", length = 200)
    private String requestedBy;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkRequest() {
    }

    public WorkRequest(
            Client client,
            String title,
            String description,
            WorkRequestStatus status,
            WorkRequestPriority priority,
            String requestedBy,
            LocalDate dueDate
    ) {
        this.client = client;
        this.title = title;
        this.description = description;
        this.status = status == null ? WorkRequestStatus.NEW : status;
        this.priority = priority == null ? WorkRequestPriority.MEDIUM : priority;
        this.requestedBy = requestedBy;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
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

    public WorkRequestStatus getStatus() {
        return status;
    }

    public void setStatus(WorkRequestStatus status) {
        this.status = status == null ? WorkRequestStatus.NEW : status;
    }

    public WorkRequestPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkRequestPriority priority) {
        this.priority = priority == null ? WorkRequestPriority.MEDIUM : priority;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
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
