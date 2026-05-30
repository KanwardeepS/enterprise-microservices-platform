package com.company.platform.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Health indicator that checks Kafka broker connectivity.
 * 
 * Verifies:
 * - Kafka brokers are reachable
 * - Cluster metadata is accessible
 * - Connection can be established within timeout
 * 
 * Only active when Kafka is configured as the eventing provider.
 */
@Component
@ConditionalOnClass(KafkaAdmin.class)
@ConditionalOnProperty(name = "platform.eventing.provider", havingValue = "kafka", matchIfMissing = true)
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final KafkaAdmin kafkaAdmin;
    private static final int TIMEOUT_SECONDS = 5;
    
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                // Describe cluster to verify connectivity
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                
                // Wait for result with timeout
                String clusterId = clusterResult.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                int nodeCount = clusterResult.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();
                
                long duration = System.currentTimeMillis() - startTime;
                
                return Health.up()
                    .withDetail("kafka", "connected")
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .withDetail("responseTimeMs", duration)
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("kafka", "unreachable")
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
