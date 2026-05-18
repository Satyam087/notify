package com.npaas.notify.templates;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.npaas.notify.common.security.TenantAuthorizationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/templates")
@Validated
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public NotificationTemplateController(
            NotificationTemplateService notificationTemplateService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.notificationTemplateService = notificationTemplateService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @GetMapping
    public List<NotificationTemplateResponse> list(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$") String tenantId) {
        tenantAuthorizationService.requireTenant(tenantId);
        return notificationTemplateService.list(tenantId);
    }

    @PutMapping("/{templateKey}")
    public NotificationTemplateResponse upsert(
            @PathVariable @NotBlank @Size(max = 160) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,159}$") String templateKey,
            @Valid @RequestBody UpsertNotificationTemplateRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        return notificationTemplateService.upsert(templateKey, request);
    }

    @PostMapping("/{templateKey}/render-test")
    public RenderTemplateResponse render(
            @PathVariable @NotBlank @Size(max = 160) @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,159}$") String templateKey,
            @Valid @RequestBody RenderTemplateRequest request) {
        tenantAuthorizationService.requireTenant(request.tenantId());
        return notificationTemplateService.render(templateKey, request);
    }
}
