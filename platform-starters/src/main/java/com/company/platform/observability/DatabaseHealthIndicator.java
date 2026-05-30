package com.company.platform.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Health indicator that checks database connectivity and responsiveness.
 * 
 * Executes a simple validation query to verify:
 * - Database connection is available
 * - Database is responding to queries
 * - Connection pool has available connections
 * 
 * Returns UP if database is accessible, DOWN otherwise.
 */
@Component
@ConditionalOnClass(DataSource.class)
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Execute simple validation query
            Integer result = jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (result != null && result == 1) {
                return Health.up()
                    .withDetail("database", "responsive")
                    .withDetail("validationQuery", "SELECT 1 FROM DUAL")
                    .withDetail("responseTimeMs", duration)
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "unexpected result")
                    .withDetail("result", result)
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "unreachable")
                .withDetail("error", e.getClass().getSimpleName())
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
