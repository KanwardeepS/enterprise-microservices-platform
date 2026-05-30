package com.company.platform.eventing;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.StringUtils;

public class RabbitMqEventBus implements EventBus {

    private final RabbitTemplate rabbitTemplate;
    private final EventingProperties properties;

    public RabbitMqEventBus(RabbitTemplate rabbitTemplate, EventingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(EventMessage message) {
        String routingKey = StringUtils.hasText(message.destination())
                ? message.destination()
                : properties.getRabbitmq().getRoutingKey();
        rabbitTemplate.convertAndSend(
                properties.getRabbitmq().getExchange(),
                routingKey,
                message.payload(),
                rabbitMessage -> {
                    message.headers().forEach((key, value) -> rabbitMessage.getMessageProperties().setHeader(key, value));
                    return rabbitMessage;
                });
    }
}
