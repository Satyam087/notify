package com.npaas.notify.jobs;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.npaas.notify.events.QueuedNotificationEvent;

@Component
public class NotificationEventConsumer {

    private final NotificationJobService notificationJobService;

    public NotificationEventConsumer(NotificationJobService notificationJobService) {
        this.notificationJobService = notificationJobService;
    }

    @RabbitListener(queues = "${notify.rabbitmq.events.queue}")
    public void handle(QueuedNotificationEvent event) {
        notificationJobService.createInitialJobIfMissing(event);
    }
}
