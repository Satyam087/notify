package com.npaas.notify.events;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notify.events.recovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventRecoveryScheduler {

    private final NotificationEventRecoveryService notificationEventRecoveryService;
    private final Duration staleAfter;
    private final int batchSize;

    public NotificationEventRecoveryScheduler(
            NotificationEventRecoveryService notificationEventRecoveryService,
            @Value("${notify.events.recovery.stale-after-seconds:120}") long staleAfterSeconds,
            @Value("${notify.events.recovery.batch-size:50}") int batchSize) {
        this.notificationEventRecoveryService = notificationEventRecoveryService;
        this.staleAfter = Duration.ofSeconds(staleAfterSeconds);
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notify.events.recovery.fixed-delay-ms:60000}")
    public void republishStaleQueuedEvents() {
        notificationEventRecoveryService.republishStaleQueuedEvents(staleAfter, batchSize);
    }
}
