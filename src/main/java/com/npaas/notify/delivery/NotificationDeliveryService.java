package com.npaas.notify.delivery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.events.NotificationEventRepository;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;
import com.npaas.notify.jobs.NotificationJobStatus;

@Service
public class NotificationDeliveryService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;
    private final Map<NotificationChannel, NotificationDeliveryHandler> handlers;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public NotificationDeliveryService(
            NotificationJobRepository notificationJobRepository,
            NotificationEventRepository notificationEventRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository,
            List<NotificationDeliveryHandler> deliveryHandlers,
            @Value("${notify.delivery.max-attempts:3}") int maxAttempts,
            @Value("${notify.delivery.retry-backoff-seconds:60}") long retryBackoffSeconds) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.handlers = toHandlerMap(deliveryHandlers);
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofSeconds(retryBackoffSeconds);
    }

    @Transactional
    public int deliverDueJobs(int batchSize) {
        List<NotificationJob> jobs = findDueJobs(batchSize);
        for (NotificationJob job : jobs) {
            deliver(job);
        }

        return jobs.size();
    }

    private List<NotificationJob> findDueJobs(int batchSize) {
        List<NotificationJob> jobs = new ArrayList<>();
        jobs.addAll(notificationJobRepository.findByStatusAndNextAttemptAtIsNullOrderByCreatedAtAsc(
            NotificationJobStatus.PENDING,
            PageRequest.of(0, batchSize)
        ));

        int remaining = batchSize - jobs.size();
        if (remaining > 0) {
            jobs.addAll(notificationJobRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NotificationJobStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, remaining)
            ));
        }

        return jobs;
    }

    private void deliver(NotificationJob job) {
        NotificationEvent event = notificationEventRepository.findById(job.getEventId()).orElse(null);
        if (event == null) {
            markFailed(job, "Event no longer exists", false);
            return;
        }

        NotificationDeliveryHandler handler = handlers.get(job.getChannel());
        if (handler == null) {
            int attemptNumber = job.getAttempts() + 1;
            deliveryAttemptRepository.save(NotificationDeliveryAttempt.failed(
                job.getId(),
                job.getTenantSlug(),
                job.getChannel(),
                attemptNumber,
                "none",
                "No delivery handler configured for channel " + job.getChannel()
            ));
            markFailed(job, "No delivery handler configured for channel " + job.getChannel(), false);
            updateEventStatus(event);
            return;
        }

        int attemptNumber = job.getAttempts() + 1;
        job.markProcessing();
        event.markProcessing();

        try {
            DeliveryResult result = handler.deliver(job, event);
            deliveryAttemptRepository.save(NotificationDeliveryAttempt.success(
                job.getId(),
                job.getTenantSlug(),
                job.getChannel(),
                attemptNumber,
                result.provider(),
                result.providerMessageId()
            ));
            job.markSent();
        } catch (DeliveryException exception) {
            deliveryAttemptRepository.save(NotificationDeliveryAttempt.failed(
                job.getId(),
                job.getTenantSlug(),
                job.getChannel(),
                attemptNumber,
                providerName(handler),
                exception.getMessage()
            ));
            markFailed(job, exception.getMessage(), exception.isRetryable());
        } catch (RuntimeException exception) {
            deliveryAttemptRepository.save(NotificationDeliveryAttempt.failed(
                job.getId(),
                job.getTenantSlug(),
                job.getChannel(),
                attemptNumber,
                providerName(handler),
                "Unexpected delivery error"
            ));
            markFailed(job, "Unexpected delivery error", true);
        }

        updateEventStatus(event);
    }

    private void markFailed(NotificationJob job, String reason, boolean retryable) {
        if (retryable && job.getAttempts() + 1 < maxAttempts) {
            job.scheduleRetry(Instant.now().plus(retryBackoff));
            return;
        }

        job.markFailed();
    }

    private void updateEventStatus(NotificationEvent event) {
        long totalJobs = notificationJobRepository.countByEventId(event.getId());
        if (totalJobs == 0) {
            event.markCompleted();
            return;
        }

        long pendingOrProcessing = notificationJobRepository.countByEventIdAndStatusIn(
            event.getId(),
            List.of(NotificationJobStatus.PENDING, NotificationJobStatus.PROCESSING)
        );
        if (pendingOrProcessing > 0) {
            event.markProcessing();
            return;
        }

        long failed = notificationJobRepository.countByEventIdAndStatusIn(
            event.getId(),
            List.of(NotificationJobStatus.FAILED)
        );
        if (failed > 0) {
            event.markFailed();
            return;
        }

        event.markCompleted();
    }

    private String providerName(NotificationDeliveryHandler handler) {
        return handler == null ? "none" : handler.channel().name().toLowerCase();
    }

    private Map<NotificationChannel, NotificationDeliveryHandler> toHandlerMap(
            List<NotificationDeliveryHandler> deliveryHandlers) {
        Map<NotificationChannel, NotificationDeliveryHandler> handlerMap = new EnumMap<>(NotificationChannel.class);
        for (NotificationDeliveryHandler handler : deliveryHandlers) {
            handlerMap.put(handler.channel(), handler);
        }

        return handlerMap;
    }
}
