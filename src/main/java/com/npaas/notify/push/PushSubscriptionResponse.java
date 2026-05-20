package com.npaas.notify.push;

import java.time.Instant;
import java.util.UUID;

public record PushSubscriptionResponse(
        UUID id,
        String tenantId,
        String userId,
        String provider,
        String endpoint,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    static PushSubscriptionResponse from(PushSubscription subscription) {
        return new PushSubscriptionResponse(
            subscription.getId(),
            subscription.getTenantSlug(),
            subscription.getRecipientUserId(),
            subscription.getProvider(),
            subscription.getEndpoint(),
            subscription.isActive(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
}
