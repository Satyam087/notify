package com.npaas.notify.events;

import java.time.Instant;
import java.util.UUID;

public record QueuedNotificationEvent(
        UUID eventId,
        String tenantId,
        String eventType,
        Instant queuedAt) {
}
