package com.company.platform.eventing;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RabbitMqEventConsumer {

    private final ObjectProvider<EventMessageHandler> handlers;

    public RabbitMqEventConsumer(ObjectProvider<EventMessageHandler> handlers) {
        this.handlers = handlers;
    }

    @RabbitListener(queues = "${platform.eventing.rabbitmq.queue:platform.events.queue}")
    public void consume(Message message) {
        Map<String, Object> headers = new HashMap<>(message.getMessageProperties().getHeaders());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        EventMessage eventMessage = new EventMessage(routingKey, null, payload, headers);
        handlers.orderedStream().forEach(handler -> handler.onMessage(eventMessage));
    }
}
