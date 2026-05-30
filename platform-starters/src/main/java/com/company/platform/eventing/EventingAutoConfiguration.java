package com.company.platform.eventing;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@EnableKafka
@EnableRabbit
@EnableConfigurationProperties(EventingProperties.class)
public class EventingAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "kafka", matchIfMissing = true)
    @ConditionalOnMissingBean
    public EventBus kafkaEventBus(KafkaTemplate<String, String> kafkaTemplate, EventingProperties properties) {
        return new KafkaEventBus(kafkaTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "rabbitmq")
    @ConditionalOnMissingBean
    public EventBus rabbitMqEventBus(RabbitTemplate rabbitTemplate, EventingProperties properties) {
        return new RabbitMqEventBus(rabbitTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "kafka", matchIfMissing = true)
    @ConditionalOnMissingBean
    public KafkaEventConsumer kafkaEventConsumer(org.springframework.beans.factory.ObjectProvider<EventMessageHandler> handlers) {
        return new KafkaEventConsumer(handlers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "rabbitmq")
    @ConditionalOnMissingBean
    public RabbitMqEventConsumer rabbitMqEventConsumer(org.springframework.beans.factory.ObjectProvider<EventMessageHandler> handlers) {
        return new RabbitMqEventConsumer(handlers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "rabbitmq")
    @ConditionalOnMissingBean(name = "platformEventsExchange")
    public DirectExchange platformEventsExchange(EventingProperties properties) {
        return new DirectExchange(properties.getRabbitmq().getExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "rabbitmq")
    @ConditionalOnMissingBean(name = "platformEventsQueue")
    public Queue platformEventsQueue(EventingProperties properties) {
        return new Queue(properties.getRabbitmq().getQueue(), true);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.eventing", name = "provider", havingValue = "rabbitmq")
    @ConditionalOnMissingBean(name = "platformEventsBinding")
    public Binding platformEventsBinding(Queue platformEventsQueue,
                                         DirectExchange platformEventsExchange,
                                         EventingProperties properties) {
        return BindingBuilder.bind(platformEventsQueue)
                .to(platformEventsExchange)
                .with(properties.getRabbitmq().getRoutingKey());
    }
}
