package com.npaas.notify.jobs;

import java.util.List;

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
@RequestMapping("/api/v1/jobs")
@Validated
public class NotificationJobController {

    private final FailedNotificationJobService failedNotificationJobService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public NotificationJobController(
            FailedNotificationJobService failedNotificationJobService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.failedNotificationJobService = failedNotificationJobService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @GetMapping("/failed")
    public List<FailedNotificationJobResponse> failed(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        tenantAuthorizationService.requireTenant(tenantId);
        return failedNotificationJobService.listFailed(tenantId, limit);
    }
}
