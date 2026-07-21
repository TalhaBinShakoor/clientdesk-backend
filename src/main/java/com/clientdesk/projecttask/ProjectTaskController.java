package com.clientdesk.projecttask;

import com.clientdesk.api.ApiPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Validated
@RestController
@RequestMapping("/api/project-tasks")
public class ProjectTaskController {

    private final ProjectTaskService projectTaskService;

    public ProjectTaskController(ProjectTaskService projectTaskService) {
        this.projectTaskService = projectTaskService;
    }

    @GetMapping
    public ApiPage<ProjectTaskResponse> findAll(
            @RequestParam(required = false) ProjectTaskStatus status,
            @RequestParam(required = false) @Size(max = 200) String assignee,
            @RequestParam(required = false) UUID workRequestId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return projectTaskService.findAll(status, assignee, workRequestId, page, size);
    }

    @GetMapping("/{id}")
    public ProjectTaskResponse findById(@PathVariable UUID id) {
        return projectTaskService.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public ProjectTaskResponse create(@Valid @RequestBody ProjectTaskCreateRequest request) {
        return projectTaskService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectTaskResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectTaskUpdateRequest request
    ) {
        return projectTaskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProjectTaskResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectTaskStatusUpdateRequest request
    ) {
        return projectTaskService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectTaskService.delete(id);
    }
}
