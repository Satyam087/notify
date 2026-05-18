package com.npaas.notify.delivery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.events.NotificationEventRepository;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;
import com.npaas.notify.jobs.NotificationJobStatus;

@Service
class NotificationDeliveryTransactionService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    private final int maxAttempts;
    private final Duration retryBackoff;

    NotificationDeliveryTransactionService(
            NotificationJobRepository notificationJobRepository,
            NotificationEventRepository notificationEventRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository,
            @Value("${notify.delivery.max-attempts:3}") int maxAttempts,
            @Value("${notify.delivery.retry-backoff-seconds:60}") long retryBackoffSeconds) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofSeconds(retryBackoffSeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedNotificationJob> claim(UUID jobId) {
        Optional<NotificationJob> maybeJob = notificationJobRepository.findByIdForUpdate(jobId);
        if (maybeJob.isEmpty()) {
            return Optional.empty();
        }

        NotificationJob job = maybeJob.get();
        if (!isDue(job)) {
            return Optional.empty();
        }

        NotificationEvent event = notificationEventRepository.findById(job.getEventId()).orElse(null);
        if (event == null) {
            int attemptNumber = job.getAttempts() + 1;
            deliveryAttemptRepository.save(NotificationDeliveryAttempt.failed(
                job.getId(),
                job.getTenantSlug(),
                job.getChannel(),
                attemptNumber,
                "none",
                "Event no longer exists"
            ));
            job.markFailed();
            return Optional.empty();
        }

        int attemptNumber = job.getAttempts() + 1;
        job.markProcessing();
        event.markProcessing();
        return Optional.of(new ClaimedNotificationJob(job, event, attemptNumber));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(ClaimedNotificationJob claimedJob, DeliveryResult result) {
        NotificationJob job = notificationJobRepository.findByIdForUpdate(claimedJob.job().getId()).orElse(null);
        if (job == null || job.getStatus() != NotificationJobStatus.PROCESSING) {
            return;
        }

        deliveryAttemptRepository.save(NotificationDeliveryAttempt.success(
            job.getId(),
            job.getTenantSlug(),
            job.getChannel(),
            claimedJob.attemptNumber(),
            result.provider(),
            result.providerMessageId()
        ));
        job.markSent();
        updateEventStatus(job.getEventId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(ClaimedNotificationJob claimedJob, String provider, String reason, boolean retryable) {
        NotificationJob job = notificationJobRepository.findByIdForUpdate(claimedJob.job().getId()).orElse(null);
        if (job == null || job.getStatus() != NotificationJobStatus.PROCESSING) {
            return;
        }

        deliveryAttemptRepository.save(NotificationDeliveryAttempt.failed(
            job.getId(),
            job.getTenantSlug(),
            job.getChannel(),
            claimedJob.attemptNumber(),
            provider,
            reason
        ));
        markFailed(job, retryable);
        updateEventStatus(job.getEventId());
    }

    private boolean isDue(NotificationJob job) {
        if (job.getStatus() != NotificationJobStatus.PENDING) {
            return false;
        }

        Instant nextAttemptAt = job.getNextAttemptAt();
        return nextAttemptAt == null || !nextAttemptAt.isAfter(Instant.now());
    }

    private void markFailed(NotificationJob job, boolean retryable) {
        if (retryable && job.getAttempts() + 1 < maxAttempts) {
            job.scheduleRetry(Instant.now().plus(retryBackoff));
            return;
        }

        job.markFailed();
    }

    private void updateEventStatus(UUID eventId) {
        NotificationEvent event = notificationEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }

        long totalJobs = notificationJobRepository.countByEventId(eventId);
        if (totalJobs == 0) {
            event.markCompleted();
            return;
        }

        long pendingOrProcessing = notificationJobRepository.countByEventIdAndStatusIn(
            eventId,
            List.of(NotificationJobStatus.PENDING, NotificationJobStatus.PROCESSING)
        );
        if (pendingOrProcessing > 0) {
            event.markProcessing();
            return;
        }

        long failed = notificationJobRepository.countByEventIdAndStatusIn(
            eventId,
            List.of(NotificationJobStatus.FAILED)
        );
        if (failed > 0) {
            event.markFailed();
            return;
        }

        event.markCompleted();
    }
}
