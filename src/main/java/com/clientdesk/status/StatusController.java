package com.clientdesk.status;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")
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