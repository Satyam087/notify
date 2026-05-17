package com.npaas.notify.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitMqConfig {

    @Bean
    TopicExchange notificationEventsExchange(@Value("${notify.rabbitmq.events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue notificationEventsQueue(@Value("${notify.rabbitmq.events.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding notificationEventsBinding(
            TopicExchange notificationEventsExchange,
            Queue notificationEventsQueue,
            @Value("${notify.rabbitmq.events.routing-key}") String routingKey) {
        return BindingBuilder
            .bind(notificationEventsQueue)
            .to(notificationEventsExchange)
            .with(routingKey);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
