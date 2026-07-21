package com.clientdesk.status;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StatusController {

    @GetMapping("/api/status")
    Map<String, Object> status() {
        return Map.of(
                "status", "UP",
                "service", "clientdesk-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
