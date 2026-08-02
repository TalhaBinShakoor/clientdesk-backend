package com.clientdesk.ai;

public record AiAssistantResponse(
        String content,
        AiAssistantSource source
) {
}
