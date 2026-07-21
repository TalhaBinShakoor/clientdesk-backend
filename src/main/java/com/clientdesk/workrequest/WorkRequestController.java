package com.clientdesk.workrequest;

import com.clientdesk.api.ApiPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/work-requests")
public class WorkRequestController {

    private final WorkRequestService workRequestService;

    public WorkRequestController(WorkRequestService workRequestService) {
        this.workRequestService = workRequestService;
    }

    @GetMapping
    public ApiPage<WorkRequestResponse> findAll(
            @RequestParam(required = false) WorkRequestStatus status,
            @RequestParam(required = false) WorkRequestPriority priority,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return workRequestService.findAll(status, priority, clientId, page, size);
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
