package com.npaas.notify.jobs;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.delivery.NotificationDeliveryAttemptRepository;

@Service
public class FailedNotificationJobService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;

    public FailedNotificationJobService(
            NotificationJobRepository notificationJobRepository,
            NotificationDeliveryAttemptRepository deliveryAttemptRepository) {
        this.notificationJobRepository = notificationJobRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
    }

    @Transactional(readOnly = true)
    public List<FailedNotificationJobResponse> listFailed(String tenantId, int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));

        return notificationJobRepository
            .findByTenantSlugAndStatusOrderByUpdatedAtDesc(tenantId, NotificationJobStatus.FAILED, pageable)
            .stream()
            .map(job -> FailedNotificationJobResponse.from(
                job,
                deliveryAttemptRepository.findFirstByJobIdOrderByAttemptNumberDesc(job.getId()).orElse(null)
            ))
            .toList();
    }
}
