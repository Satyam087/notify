package com.npaas.notify.events;

import com.npaas.notify.common.security.TenantAuthorizationService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/events")
public class NotificationEventController {

    private final NotificationEventService notificationEventService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public NotificationEventController(
            NotificationEventService notificationEventService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.notificationEventService = notificationEventService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestEventResponse ingest(@Valid @RequestBody IngestEventRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        return notificationEventService.ingest(request);
    }
}
