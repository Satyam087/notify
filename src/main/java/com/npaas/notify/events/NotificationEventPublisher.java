package com.npaas.notify.events;

import java.time.Instant;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public NotificationEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${notify.rabbitmq.events.exchange}") String exchangeName,
            @Value("${notify.rabbitmq.events.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public void publish(NotificationEvent event) {
        QueuedNotificationEvent queuedEvent = new QueuedNotificationEvent(
            event.getId(),
            event.getTenantSlug(),
            event.getEventType(),
            Instant.now()
        );

        rabbitTemplate.convertAndSend(exchangeName, routingKey, queuedEvent);
    }
}
