package com.company.platform.eventing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.eventing")
public class EventingProperties {

    private String provider = "kafka";
    private String defaultDestination = "platform.events";
    private final Kafka kafka = new Kafka();
    private final Rabbitmq rabbitmq = new Rabbitmq();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDefaultDestination() {
        return defaultDestination;
    }

    public void setDefaultDestination(String defaultDestination) {
        this.defaultDestination = defaultDestination;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public Rabbitmq getRabbitmq() {
        return rabbitmq;
    }

    public static class Kafka {
        private String topic = "platform.events";
        private String consumerGroup = "platform-service";

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }
    }

    public static class Rabbitmq {
        private String exchange = "platform.events";
        private String queue = "platform.events.queue";
        private String routingKey = "platform.events.key";

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }
    }
}
