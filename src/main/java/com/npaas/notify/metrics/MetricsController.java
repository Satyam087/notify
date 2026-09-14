package com.npaas.notify.metrics;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.npaas.notify.common.security.TenantAuthorizationService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/metrics")
@Validated
public class MetricsController {

    private final MetricsService metricsService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public MetricsController(MetricsService metricsService, TenantAuthorizationService tenantAuthorizationService) {
        this.metricsService = metricsService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @GetMapping
    public MetricsResponse metrics(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        tenantAuthorizationService.requireTenant(tenantId);
        return metricsService.metrics(tenantId, days);
    }
}
