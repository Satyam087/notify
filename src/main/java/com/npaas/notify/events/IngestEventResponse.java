package com.npaas.notify.events;

import java.time.Instant;
import java.util.UUID;

public record IngestEventResponse(
        UUID eventId,
        String tenantId,
        String eventType,
        String status,
        boolean duplicate,
        Instant createdAt) {
}
