package com.npaas.notify.events;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventRecoveryService.class);

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

        int republished = 0;
        for (NotificationEvent event : events) {
            try {
                notificationEventPublisher.publish(event);
                republished += 1;
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to republish queued notification event {}", event.getId(), exception);
            }
        }

        return republished;
    }
}
