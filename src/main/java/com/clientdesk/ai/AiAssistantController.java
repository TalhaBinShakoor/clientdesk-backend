package com.clientdesk.ai;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/work-requests/{workRequestId}/summary")
    public AiAssistantResponse summarizeRequestThread(@PathVariable UUID workRequestId) {
        return aiAssistantService.summarizeRequestThread(workRequestId);
    }

    @PostMapping("/work-requests/{workRequestId}/draft-reply")
    public AiAssistantResponse draftClientReply(
            @PathVariable UUID workRequestId,
            @RequestBody(required = false) DraftClientReplyRequest request
    ) {
        return aiAssistantService.draftClientReply(workRequestId, request);
    }
}
