package com.npaas.notify.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.npaas.notify.jobs.NotificationJob;

public record NotificationJobStatusResponse(
        UUID id,
        String channel,
        String status,
        int attempts,
        Instant nextAttemptAt,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryAttemptStatusResponse> deliveryAttempts) {

    public static NotificationJobStatusResponse from(
            NotificationJob job,
            List<DeliveryAttemptStatusResponse> deliveryAttempts) {
        return new NotificationJobStatusResponse(
            job.getId(),
            job.getChannel().name(),
            job.getStatus().name(),
            job.getAttempts(),
            job.getNextAttemptAt(),
            job.getCreatedAt(),
            job.getUpdatedAt(),
            deliveryAttempts
        );
    }
}
