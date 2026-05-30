
package com.company.service.controller;

import com.company.platform.eventing.EventBus;
import com.company.platform.eventing.EventMessage;
import com.company.platform.security.RequireRoles;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sample REST controller demonstrating observability features.
 * 
 * All endpoints are automatically instrumented with:
 * - Correlation ID tracking (via CorrelationIdFilter)
 * - Distributed tracing (via OpenTelemetry)
 * - Request metrics (via @Timed and Spring actuator)
 * - Structured JSON logging with MDC fields
 */
@RestController
public class SampleController {
    
    private static final Logger log = LoggerFactory.getLogger(SampleController.class);

    private final EventBus eventBus;

    public SampleController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping("/api/sample")
    @Timed(value = "api.sample.requests", description = "Time taken to process sample requests", percentiles = {0.5, 0.95, 0.99})
    public String sample() {
        log.info("Processing sample request");
        return "Hello from production template";
    }

    @GetMapping("/api/secure")
    @RequireRoles({"USER", "ADMIN"})
    @Timed(value = "api.secure.requests", description = "Time taken to process secure requests", percentiles = {0.5, 0.95, 0.99})
    public String secure() {
        log.info("Processing secure request");
        return "Secure endpoint response";
    }

    @GetMapping("/api/admin")
    @RequireRoles("ADMIN")
    @Timed(value = "api.admin.requests", description = "Time taken to process admin requests", percentiles = {0.5, 0.95, 0.99})
    public String admin() {
        log.info("Processing admin request");
        return "Admin endpoint response";
    }

    @PostMapping("/api/events/{destination}")
    @RequireRoles({"USER", "ADMIN"})
    @Timed(value = "api.events.publish", description = "Time taken to publish events", percentiles = {0.5, 0.95, 0.99})
    public ResponseEntity<Void> publish(@PathVariable String destination, @RequestBody String payload) {
        log.info("Publishing event to destination: {}", destination);
        
        try {
            eventBus.publish(new EventMessage(destination, null, payload, Map.of("source", "service-template")));
            log.info("Event published successfully to destination: {}", destination);
            return ResponseEntity.accepted().build();
            
        } catch (Exception e) {
            log.error("Failed to publish event to destination: {}", destination, e);
            throw e;
        }
    }
}
