package com.npaas.notify.inapp;

import java.time.Instant;
import java.util.UUID;

public record InAppNotificationResponse(
        UUID id,
        String tenantId,
        String userId,
        String title,
        String body,
        boolean read,
        Instant readAt,
        Instant createdAt) {

    static InAppNotificationResponse from(InAppNotification notification) {
        return new InAppNotificationResponse(
            notification.getId(),
            notification.getTenantSlug(),
            notification.getRecipientUserId(),
            notification.getTitle(),
            notification.getBody(),
            notification.isRead(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
