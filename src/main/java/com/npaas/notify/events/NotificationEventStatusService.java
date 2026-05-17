package com.npaas.notify.events;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.npaas.notify.delivery.NotificationDeliveryAttemptRepository;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;

@Service
public class NotificationEventStatusService {

    private final NotificationEventRepository notificationEventRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;

    public NotificationEventStatusService(
            NotificationEventRepository notificationEventRepository,
            NotificationJobRepository notificationJobRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository) {
        this.notificationEventRepository = notificationEventRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @Transactional(readOnly = true)
    public NotificationEventStatusResponse getByIdempotencyKey(String tenantId, String idempotencyKey) {
        NotificationEvent event = notificationEventRepository
            .findByTenantSlugAndIdempotencyKey(tenantId, idempotencyKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification event not found"));

        List<NotificationJobStatusResponse> jobs = notificationJobRepository
            .findByEventIdOrderByCreatedAtAsc(event.getId())
            .stream()
            .map(this::toJobStatus)
            .toList();

        return new NotificationEventStatusResponse(
            event.getId(),
            event.getTenantSlug(),
            event.getEventType(),
            event.getIdempotencyKey(),
            event.getStatus().name(),
            event.getCreatedAt(),
            event.getUpdatedAt(),
            jobs
        );
    }

    private NotificationJobStatusResponse toJobStatus(NotificationJob job) {
        List<DeliveryAttemptStatusResponse> attempts = deliveryAttemptRepository
            .findByJobIdOrderByAttemptNumberAsc(job.getId())
            .stream()
            .map(DeliveryAttemptStatusResponse::from)
            .toList();

        return NotificationJobStatusResponse.from(job, attempts);
    }
}
