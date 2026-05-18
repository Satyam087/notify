package com.npaas.notify.templates;

import java.time.Instant;
import java.util.UUID;

import com.npaas.notify.jobs.NotificationChannel;

public record NotificationTemplateResponse(
        UUID id,
        String tenantId,
        String eventType,
        NotificationChannel channel,
        String templateKey,
        String subjectTemplate,
        String bodyTemplate,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    static NotificationTemplateResponse from(NotificationTemplate template) {
        return new NotificationTemplateResponse(
            template.getId(),
            template.getTenantSlug(),
            template.getEventType(),
            template.getChannel(),
            template.getTemplateKey(),
            template.getSubjectTemplate(),
            template.getBodyTemplate(),
            template.isEnabled(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }
}
