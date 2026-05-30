package com.company.platform.eventing;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public class KafkaEventBus implements EventBus {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventingProperties properties;

    public KafkaEventBus(KafkaTemplate<String, String> kafkaTemplate, EventingProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(EventMessage message) {
        String destination = StringUtils.hasText(message.destination())
                ? message.destination()
                : properties.getKafka().getTopic();
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(destination, message.key(), message.payload());
        message.headers().forEach((key, value) ->
                producerRecord.headers().add(new RecordHeader(key, String.valueOf(value).getBytes(StandardCharsets.UTF_8))));
        kafkaTemplate.send(producerRecord);
    }
}
