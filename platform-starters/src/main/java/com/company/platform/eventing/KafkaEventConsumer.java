package com.company.platform.eventing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.annotation.KafkaListener;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class KafkaEventConsumer {

    private final ObjectProvider<EventMessageHandler> handlers;

    public KafkaEventConsumer(ObjectProvider<EventMessageHandler> handlers) {
        this.handlers = handlers;
    }

    @KafkaListener(
            topics = "${platform.eventing.kafka.topic:platform.events}",
            groupId = "${platform.eventing.kafka.consumer-group:platform-service}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        Map<String, Object> headers = new HashMap<>();
        record.headers().forEach(header -> {
            if (header.value() != null) {
                headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
        });
        EventMessage message = new EventMessage(record.topic(), record.key(), record.value(), headers);
        handlers.orderedStream().forEach(handler -> handler.onMessage(message));
    }
}
