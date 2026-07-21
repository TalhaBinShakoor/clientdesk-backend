package com.clientdesk.activity;

import com.clientdesk.api.ApiPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/activity-events")
public class ActivityEventController {

    private final ActivityEventService activityEventService;

    public ActivityEventController(ActivityEventService activityEventService) {
        this.activityEventService = activityEventService;
    }

    @GetMapping
    public ApiPage<ActivityEventResponse> findForWorkRequest(
            @RequestParam UUID workRequestId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return activityEventService.findForWorkRequest(workRequestId, page, size);
    }
}
