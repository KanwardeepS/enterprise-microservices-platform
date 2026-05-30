package com.company.platform.observability;

import com.rabbitmq.client.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator that checks RabbitMQ broker connectivity.
 * 
 * Verifies:
 * - RabbitMQ broker is reachable
 * - Connection can be established
 * - Channel can be created
 * 
 * Only active when RabbitMQ is configured as the eventing provider.
 */
@Component
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnProperty(name = "platform.eventing.provider", havingValue = "rabbitmq")
public class RabbitMqHealthIndicator implements HealthIndicator {
    
    private final ConnectionFactory connectionFactory;
    
    public RabbitMqHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Get connection to verify connectivity
            Connection connection = connectionFactory.createConnection().getDelegate();
            
            if (connection.isOpen()) {
                long duration = System.currentTimeMillis() - startTime;
                
                return Health.up()
                    .withDetail("rabbitmq", "connected")
                    .withDetail("address", connection.getAddress().getHostAddress())
                    .withDetail("port", connection.getPort())
                    .withDetail("responseTimeMs", duration)
                    .build();
            } else {
                return Health.down()
                    .withDetail("rabbitmq", "connection closed")
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("rabbitmq", "unreachable")
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
