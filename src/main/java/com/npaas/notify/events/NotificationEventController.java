package com.npaas.notify.events;

import com.npaas.notify.common.security.TenantAuthorizationService;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/events")
@Validated
public class NotificationEventController {

    private final NotificationEventService notificationEventService;
    private final NotificationEventStatusService notificationEventStatusService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public NotificationEventController(
            NotificationEventService notificationEventService,
            NotificationEventStatusService notificationEventStatusService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.notificationEventService = notificationEventService;
        this.notificationEventStatusService = notificationEventStatusService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestEventResponse ingest(@Valid @RequestBody IngestEventRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        return notificationEventService.ingest(request);
    }

    @GetMapping("/status")
    public NotificationEventStatusResponse status(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId,
            @RequestParam @NotBlank @Size(max = 180) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,179}$") String idempotencyKey) {
        tenantAuthorizationService.requireTenant(tenantId);
        return notificationEventStatusService.getByIdempotencyKey(tenantId, idempotencyKey);
    }
}
