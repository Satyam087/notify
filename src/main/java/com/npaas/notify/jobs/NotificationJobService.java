package com.npaas.notify.jobs;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.npaas.notify.events.QueuedNotificationEvent;

@Service
public class NotificationJobService {

    private static final NotificationChannel DEFAULT_CHANNEL = NotificationChannel.IN_APP;

    private final NotificationJobRepository notificationJobRepository;

    public NotificationJobService(NotificationJobRepository notificationJobRepository) {
        this.notificationJobRepository = notificationJobRepository;
    }

    @Transactional
    public void createInitialJobIfMissing(QueuedNotificationEvent event) {
        if (notificationJobRepository.existsByEventIdAndChannel(event.eventId(), DEFAULT_CHANNEL)) {
            return;
        }

        try {
            NotificationJob job = new NotificationJob(
                UUID.randomUUID(),
                event.eventId(),
                event.tenantId(),
                DEFAULT_CHANNEL,
                NotificationJobStatus.PENDING
            );
            notificationJobRepository.save(job);
        } catch (DataIntegrityViolationException ignored) {
            // A redelivered or concurrent message may race this insert. The unique
            // event/channel constraint keeps the worker idempotent.
        }
    }
}
