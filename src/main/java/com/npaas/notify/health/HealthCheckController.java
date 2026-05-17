package com.npaas.notify.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/api/health-check")
    public Map<String, String> healthCheck() {
        return Map.of(
            "status", "ok",
            "service", "notify",
            "version", "0.0.1"
        );
    }
}
