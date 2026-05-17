package com.npaas.notify.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotificationEventStatusResponse(
        UUID id,
        String tenantId,
        String eventType,
        String idempotencyKey,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<NotificationJobStatusResponse> jobs) {
}
