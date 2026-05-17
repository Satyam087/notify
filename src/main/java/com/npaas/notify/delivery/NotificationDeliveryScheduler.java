package com.npaas.notify.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notify.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationDeliveryScheduler {

    private final NotificationDeliveryService notificationDeliveryService;
    private final int batchSize;

    public NotificationDeliveryScheduler(
            NotificationDeliveryService notificationDeliveryService,
            @Value("${notify.delivery.batch-size:50}") int batchSize) {
        this.notificationDeliveryService = notificationDeliveryService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notify.delivery.fixed-delay-ms:5000}")
    public void deliverDueJobs() {
        notificationDeliveryService.deliverDueJobs(batchSize);
    }
}
