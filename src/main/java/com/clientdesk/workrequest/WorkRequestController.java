package com.clientdesk.workrequest;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/work-requests")
public class WorkRequestController {

    private final WorkRequestService workRequestService;

    public WorkRequestController(WorkRequestService workRequestService) {
        this.workRequestService = workRequestService;
    }

    @GetMapping
    public List<WorkRequestResponse> findAll(
            @RequestParam(required = false) WorkRequestStatus status,
            @RequestParam(required = false) WorkRequestPriority priority,
            @RequestParam(required = false) UUID clientId
    ) {
        return workRequestService.findAll(status, priority, clientId);
    }

    @GetMapping("/{id}")
    public WorkRequestResponse findById(@PathVariable UUID id) {
        return workRequestService.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public WorkRequestResponse create(@Valid @RequestBody WorkRequestCreateRequest request) {
        return workRequestService.create(request);
    }

    @PutMapping("/{id}")
    public WorkRequestResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkRequestUpdateRequest request
    ) {
        return workRequestService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public WorkRequestResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody WorkRequestStatusUpdateRequest request
    ) {
        return workRequestService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        workRequestService.delete(id);
    }
}
