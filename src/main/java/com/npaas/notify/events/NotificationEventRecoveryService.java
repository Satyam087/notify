package com.npaas.notify.events;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventRecoveryService {

    private final NotificationEventRepository notificationEventRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public NotificationEventRecoveryService(
            NotificationEventRepository notificationEventRepository,
            NotificationEventPublisher notificationEventPublisher) {
        this.notificationEventRepository = notificationEventRepository;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional(readOnly = true)
    public int republishStaleQueuedEvents(Duration staleAfter, int batchSize) {
        Instant staleBefore = Instant.now().minus(staleAfter);
        List<NotificationEvent> events = notificationEventRepository
            .findByStatusAndUpdatedAtBeforeOrderByCreatedAtAsc(
                NotificationEventStatus.QUEUED,
                staleBefore,
                PageRequest.of(0, batchSize)
            );

        for (NotificationEvent event : events) {
            notificationEventPublisher.publish(event);
        }

        return events.size();
    }
}
