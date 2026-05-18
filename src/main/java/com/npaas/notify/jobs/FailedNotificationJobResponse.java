package com.npaas.notify.jobs;

import java.time.Instant;
import java.util.UUID;

import com.npaas.notify.delivery.NotificationDeliveryAttempt;

public record FailedNotificationJobResponse(
        UUID id,
        UUID eventId,
        String tenantId,
        NotificationChannel channel,
        int attempts,
        String renderedSubject,
        String lastProvider,
        String lastError,
        Instant lastAttemptedAt,
        Instant createdAt,
        Instant updatedAt) {

    static FailedNotificationJobResponse from(NotificationJob job, NotificationDeliveryAttempt lastAttempt) {
        return new FailedNotificationJobResponse(
            job.getId(),
            job.getEventId(),
            job.getTenantSlug(),
            job.getChannel(),
            job.getAttempts(),
            job.getRenderedSubject(),
            lastAttempt == null ? null : lastAttempt.getProvider(),
            lastAttempt == null ? null : lastAttempt.getErrorMessage(),
            lastAttempt == null ? null : lastAttempt.getAttemptedAt(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }
}
