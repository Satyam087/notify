package com.npaas.notify.events;

import java.time.Instant;
import java.util.UUID;

import com.npaas.notify.delivery.NotificationDeliveryAttempt;

public record DeliveryAttemptStatusResponse(
        UUID id,
        int attemptNumber,
        String status,
        String provider,
        String providerMessageId,
        String errorMessage,
        Instant attemptedAt) {

    public static DeliveryAttemptStatusResponse from(NotificationDeliveryAttempt attempt) {
        return new DeliveryAttemptStatusResponse(
            attempt.getId(),
            attempt.getAttemptNumber(),
            attempt.getStatus().name(),
            attempt.getProvider(),
            attempt.getProviderMessageId(),
            attempt.getErrorMessage(),
            attempt.getAttemptedAt()
        );
    }
}
