package com.npaas.notify.templates;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final TemplateRenderer templateRenderer;

    public NotificationTemplateService(
            NotificationTemplateRepository notificationTemplateRepository,
            TemplateRenderer templateRenderer) {
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.templateRenderer = templateRenderer;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> list(String tenantId) {
        return notificationTemplateRepository.findByTenantSlugOrderByEventTypeAscChannelAscTemplateKeyAsc(tenantId)
            .stream()
            .map(NotificationTemplateResponse::from)
            .toList();
    }

    @Transactional
    public NotificationTemplateResponse upsert(String templateKey, UpsertNotificationTemplateRequest request) {
        NotificationTemplate template = notificationTemplateRepository
            .findByTenantSlugAndTemplateKey(request.tenantId(), templateKey)
            .map(existing -> {
                existing.update(
                    request.eventType(),
                    request.channel(),
                    request.subjectTemplate(),
                    request.bodyTemplate(),
                    request.enabled()
                );
                return existing;
            })
            .orElseGet(() -> new NotificationTemplate(
                UUID.randomUUID(),
                request.tenantId(),
                request.eventType(),
                request.channel(),
                templateKey,
                request.subjectTemplate(),
                request.bodyTemplate(),
                request.enabled()
            ));

        return NotificationTemplateResponse.from(notificationTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public RenderTemplateResponse render(String templateKey, RenderTemplateRequest request) {
        NotificationTemplate template = notificationTemplateRepository
            .findByTenantSlugAndTemplateKey(request.tenantId(), templateKey)
            .orElseThrow(() -> new NotificationTemplateNotFoundException(request.tenantId(), templateKey));

        RenderedTemplate renderedTemplate = templateRenderer.render(template, request.payload().toString());
        return new RenderTemplateResponse(renderedTemplate.subject(), renderedTemplate.body());
    }
}
