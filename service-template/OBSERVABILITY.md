# Observability Guide

## Platform Service Template - Production-Ready Observability

**Version:** 1.0.0  
**Last Updated:** April 2026

---

## Table of Contents

1. [Overview & Quick Start](#1-overview--quick-start)
2. [Architecture Overview](#2-architecture-overview)
3. [Structured Logging Guide](#3-structured-logging-guide)
4. [Distributed Tracing Guide](#4-distributed-tracing-guide)
5. [Metrics & Monitoring Guide](#5-metrics--monitoring-guide)
6. [Health Checks & Probes](#6-health-checks--probes)
7. [Configuration Reference](#7-configuration-reference)
8. [Common Patterns & Recipes](#8-common-patterns--recipes)
9. [Troubleshooting Guide](#9-troubleshooting-guide)
10. [Migration Guide](#10-migration-guide)
11. [Integration Examples](#11-integration-examples)
12. [Best Practices & Guidelines](#12-best-practices--guidelines)
13. [FAQ](#13-faq)
14. [Additional Resources](#14-additional-resources)

---

## 1. Overview & Quick Start

### What's Included Out-of-the-Box

This service template provides **enterprise-grade observability** built-in, following the three-pillar model:

| Pillar | Features | Auto-Configured |
|--------|----------|-----------------|
| **📋 Logging** | Structured JSON logs with correlation IDs, MDC fields, async propagation | ✅ Yes |
| **📊 Metrics** | Prometheus metrics, JVM instrumentation, custom business metrics | ✅ Yes |
| **🔍 Tracing** | OpenTelemetry distributed tracing, W3C trace context, span creation | ✅ Yes |
| **❤️ Health** | Kubernetes probes, custom health indicators (DB, Kafka, RabbitMQ) | ✅ Yes |

### 30-Second Quick Start

1. **Clone the template:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

2. **View metrics:**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. **Check health:**
   ```bash
   curl http://localhost:8080/actuator/health/readiness
   ```

4. **Test correlation ID:**
   ```bash
   curl -H "X-Correlation-ID: test-123" http://localhost:8080/api/sample
   # Check logs - you'll see correlationId: "test-123" in all log entries
   ```

### 30-Second Demo: Correlation in Action

```bash
# Send request with correlation ID
curl -H "X-Correlation-ID: demo-abc-123" http://localhost:8080/api/sample

# Response header includes the same correlation ID
X-Correlation-ID: demo-abc-123

# All logs for this request include the correlation ID:
{
  "timestamp": "2026-04-20T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.company.service.controller.SampleController",
  "message": "Processing sample request",
  "correlationId": "demo-abc-123",  ← Automatically included
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "1234567890abcdef"
}
```

---

## 2. Architecture Overview

### Observability Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                      External Request                         │
└───────────────────────────┬──────────────────────────────────┘
                            │
                   ┌────────▼────────┐
                   │ CorrelationId   │  Generate/Extract UUID
                   │    Filter       │  Store in MDC
                   └────────┬────────┘
                            │
                   ┌────────▼────────┐
                   │  OTel Auto-     │  Create root span
                   │ Instrumentation │  Extract W3C trace context
                   └────────┬────────┘
                            │
                   ┌────────▼────────┐
                   │   Controller    │  @Timed metrics
                   │   (@Timed)      │  Structured logging
                   └────────┬────────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
       ┌──────▼──────┐  ┌───▼────┐  ┌────▼─────┐
       │  Database   │  │  Kafka │  │  @Async  │
       │  (Traced)   │  │(Traced)│  │  Method  │
       └──────┬──────┘  └───┬────┘  └────┬─────┘
              │             │             │
              └─────────────┼─────────────┘
                            │
                   ┌────────▼────────┐
                   │  JSON Logs      │  → ELK Stack
                   │  Metrics        │  → Prometheus
                   │  Traces         │  → Jaeger/Tempo
                   └─────────────────┘
```

### How Correlation IDs Flow Through the System

1. **HTTP Request** → Filter extracts or generates `X-Correlation-ID`
2. **MDC Storage** → Stored in `ThreadLocal` for current thread
3. **Logs** → All log statements include `correlationId` field (JSON)
4. **Async Operations** → `MdcTaskDecorator` propagates to worker threads
5. **Events** → Added to Kafka/RabbitMQ message headers
6. **Downstream Services** → Forwarded in outbound HTTP calls
7. **Response** → Returned in `X-Correlation-ID` header

### Trace Context Propagation (W3C Standard)

```
┌─────────────┐     traceparent header     ┌─────────────┐
│  Service A  │  ────────────────────────►  │  Service B  │
│ traceId: T1 │  00-T1-S1-01              │ traceId: T1 │
│ spanId: S1  │                            │ spanId: S2  │
└─────────────┘                            └─────────────┘
      │                                           │
      │ Kafka message                            │
      │ headers:                                  │
      │  traceparent: 00-T1-S1-01                │
      ▼                                           ▼
┌─────────────┐                            ┌─────────────┐
│  Consumer   │                            │  Database   │
│ traceId: T1 │                            │ (auto-span) │
│ spanId: S3  │                            └─────────────┘
└─────────────┘
```

All spans share the same `traceId` → End-to-end visibility in Jaeger UI

---

## 3. Structured Logging Guide

### JSON Log Format Specification

Every log entry is a JSON object with standard fields:

```json
{
  "timestamp": "2026-04-20T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.company.service.controller.SampleController",
  "message": "Processing sample request",
  "service": "sample-service",
  "environment": "production",
  
  // MDC Fields (automatically included)
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "spanId": "1234567890abcdef",
  "userId": "john.doe@company.com",
  "requestPath": "/api/sample",
  "requestMethod": "GET",
  
  // Exception details (if error)
  "stack_trace": "java.lang.RuntimeException: Example\n\tat ..."
}
```

### Standard MDC Fields

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| `correlationId` | UUID | `X-Correlation-ID` header or generated | Unique request identifier |
| `traceId` | string | OpenTelemetry | Distributed trace identifier |
| `spanId` | string | OpenTelemetry | Current span identifier |
| `userId` | string | JWT `sub` or `preferred_username` claim | Authenticated user |
| `requestPath` | string | HTTP request URI | Request path |
| `requestMethod` | string | HTTP method | GET, POST, etc. |

### Basic Logging Examples

#### Simple Logging
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyService.class);

public void processOrder(String orderId) {
    log.info("Processing order: {}", orderId);
    // Output: {"message": "Processing order: order-123", "correlationId": "...", ...}
}
```

#### Adding Custom MDC Fields
```java
import org.slf4j.MDC;

public void processOrder(String orderId) {
    MDC.put("orderId", orderId);  // Add custom field
    
    log.info("Order processing started");
    // Output includes: "orderId": "order-123"
    
    // Do work...
    
    MDC.remove("orderId");  // Clean up when done
}
```

#### Error Logging with Stack Traces
```java
try {
    riskyOperation();
} catch (Exception e) {
    log.error("Operation failed for order: {}", orderId, e);
    // Output includes full stack_trace field
}
```

### Environment-Specific Logging

The template uses Spring profiles to adjust logging verbosity:

| Profile | Console Output | File Output | Log Level | Format |
|---------|----------------|-------------|-----------|--------|
| `dev`, `local` | Pretty JSON | No | DEBUG | Readable multi-line |
| `test` | Compact JSON | No | DEBUG | Single-line |
| `staging`, `prod` | Compact JSON | Yes | INFO | Single-line |

**Pretty JSON Example (dev):**
```json
{
  "timestamp" : "2026-04-20T10:30:45.123Z",
  "level" : "INFO",
  "message" : "Processing order",
  "correlationId" : "abc-123"
}
```

**Compact JSON Example (prod):**
```json
{"timestamp":"2026-04-20T10:30:45.123Z","level":"INFO","message":"Processing order","correlationId":"abc-123"}
```

### Integration with ELK Stack

#### Elasticsearch Query Examples

**Find all logs for a correlation ID:**
```json
GET /logs-*/_search
{
  "query": {
    "term": { "correlationId": "550e8400-e29b-41d4-a716-446655440000" }
  },
  "sort": [{ "timestamp": "asc" }]
}
```

**Find errors for a specific user:**
```json
GET /logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "userId": "john.doe@company.com" }},
        { "term": { "level": "ERROR" }}
      ]
    }
  }
}
```

**Aggregate errors by logger:**
```json
GET /logs-*/_search
{
  "size": 0,
  "query": { "term": { "level": "ERROR" }},
  "aggs": {
    "by_logger": {
      "terms": { "field": "logger.keyword", "size": 20 }
    }
  }
}
```

---

## 4. Distributed Tracing Guide

### What is Distributed Tracing?

Distributed tracing tracks requests as they flow through multiple services, creating a **trace** (end-to-end request journey) composed of **spans** (individual operations).

**Example Trace:**
```
Trace ID: T1 (entire request lifecycle)
├─ Span S1: HTTP GET /api/orders/123 (Service A) - 250ms
│  ├─ Span S2: SELECT * FROM orders (Database) - 50ms
│  └─ Span S3: Kafka publish order.created - 10ms
└─ Span S4: Process order.created event (Service B) - 100ms
   └─ Span S5: HTTP POST /external/api (External API) - 80ms
```

### Auto-Instrumented Operations

OpenTelemetry **automatically creates spans** for:

| Operation | Span Name Example | Details Captured |
|-----------|-------------------|------------------|
| HTTP Requests | `GET /api/sample` | Method, path, status, duration |
| Database Queries | `SELECT users` | SQL statement, rows, duration |
| Kafka Produce | `send platform.events` | Topic, partition, offset |
| Kafka Consume | `receive platform.events` | Topic, partition, lag |
| RestTemplate Calls | `GET http://external.api` | URL, status, duration |
| @Async Methods | Method name | Execution time |

**No code changes needed!** Tracing is automatic for these operations.

### Creating Custom Spans

For business-critical operations, create explicit spans:

#### Using @WithSpan Annotation
```java
import io.micrometer.tracing.annotation.WithSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class OrderService {
    private final Tracer tracer;
    
    @WithSpan("process-order")  // Creates a custom span
    public void processOrder(String orderId) {
        Span span = tracer.currentSpan();
        
        // Add custom attributes to the span
        span.tag("order.id", orderId);
        span.tag("order.type", "standard");
        
        // Business logic...
        
        span.event("order-validated");  // Add event to timeline
    }
}
```

#### Manual Span Creation
```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

public void complexOperation(String id) {
    Span customSpan = tracer.nextSpan().name("complex-operation");
    
    try (Tracer.SpanInScope ws = tracer.withSpan(customSpan.start())) {
        customSpan.tag("operation.id", id);
        
        // Do work...
        
    } finally {
        customSpan.end();  // IMPORTANT: Always end spans
    }
}
```

### Trace Context Propagation

#### Across HTTP Services

Trace context is **automatically propagated** via `traceparent` header:

```java
RestTemplate restTemplate = new RestTemplate();

// Trace context automatically added to headers
ResponseEntity<String> response = restTemplate.getForEntity(
    "http://downstream-service/api/endpoint",
    String.class
);

// Downstream service receives:
// traceparent: 00-a1b2c3d4...-1234567890abcdef-01
```

#### Across Kafka/RabbitMQ Messages

**Already implemented in platform-starters!** Event messages include trace headers:

```java
// Publishing (trace context added automatically)
eventBus.publish(new EventMessage("topic", null, payload, headers));

// Consuming (trace context extracted automatically)
@KafkaListener(topics = "platform.events")
public void handle(EventMessage message) {
    // This method runs in the same trace as the publisher
    // New child span created automatically
}
```

### Viewing Traces in Jaeger UI

1. **Access Jaeger:** `http://localhost:16686`

2. **Search by Correlation ID:**
   - Service: `sample-service`
   - Tags: `correlationId=550e8400-e29b-41d4-a716-446655440000`
   - Find Traces

3. **Trace View Shows:**
   - Timeline of all spans
   - Service boundaries
   - Database queries
   - External API calls
   - Duration breakdown

**Example Jaeger Trace:**
```
sample-service: GET /api/orders/123 [250ms] ████████████████████████
  ├─ database: SELECT orders [50ms] ████
  ├─ kafka: send order.created [10ms] █
  └─ async: calculate-totals [80ms] ██████
      └─ database: SELECT items [30ms] ██
```

### Performance Considerations

#### Sampling Strategies

**Default: 100% sampling** (dev/staging) - Every request traced  
**Production: 10% sampling recommended** - Reduces overhead

```yaml
platform:
  observability:
    tracing:
      sampling-rate: 0.1  # 10% of requests
```

#### Overhead Benchmarks

| Sampling Rate | Latency Overhead | CPU Overhead |
|---------------|------------------|--------------|
| 100% | <1% (0.5ms avg) | ~2% |
| 10% | <0.1% | <0.5% |
| 1% | Negligible | Negligible |

---

## 5. Metrics & Monitoring Guide

### Available Metric Types

| Type | Use Case | Example |
|------|----------|---------|
| **Counter** | Things that only increase | `http_requests_total`, `orders_processed_count` |
| **Gauge** | Values that go up/down | `queue_depth`, `active_connections` |
| **Timer** | Duration of operations | `http_request_duration_seconds`, `order_processing_time` |
| **DistributionSummary** | Size of data | `request_size_bytes`, `batch_size` |

### Auto-Instrumented Metrics

**Out-of-the-box, you get:**

#### HTTP Metrics (from Spring Actuator)
- `http_server_requests_seconds_count` - Total requests
- `http_server_requests_seconds_sum` - Total duration
- `http_server_requests_seconds_max` - Max duration
- `http_server_requests_seconds` (histogram) - p50, p95, p99 percentiles

Tags: `method`, `uri`, `status`, `exception`

#### JVM Metrics
- `jvm_memory_used_bytes` - Heap/non-heap memory
- `jvm_gc_pause_seconds` - GC pause times
- `jvm_threads_live` - Active threads
- `process_cpu_usage` - CPU utilization

#### Database Metrics (HikariCP)
- `hikaricp_connections_active` - Active DB connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_pending` - Pending connection requests
- `hikaricp_connections_timeout_total` - Connection timeouts

#### Kafka Metrics
- `kafka_producer_record_send_total` - Messages sent
- `kafka_consumer_records_consumed_total` - Messages consumed
- `kafka_consumer_fetch_latency_avg` - Consumer fetch latency

### Adding Custom Business Metrics

#### Using @Timed Annotation

```java
import io.micrometer.core.annotation.Timed;

@Service
public class OrderService {
    
    @Timed(
        value = "order.processing",
        description = "Time taken to process orders",
        percentiles = {0.5, 0.95, 0.99}
    )
    public void processOrder(Order order) {
        // Business logic...
    }
}
```

**Generates metrics:**
- `order_processing_seconds_count` - Total calls
- `order_processing_seconds_sum` - Total time
- `order_processing_seconds_max` - Max time
- `order_processing_seconds` (histogram) - p50, p95, p99

#### Using Counter

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class PaymentService {
    private final Counter successCounter;
    private final Counter failureCounter;
    
    public PaymentService(MeterRegistry registry) {
        this.successCounter = Counter.builder("payment.processed")
            .tag("status", "success")
            .description("Successful payments")
            .register(registry);
            
        this.failureCounter = Counter.builder("payment.processed")
            .tag("status", "failure")
            .description("Failed payments")
            .register(registry);
    }
    
    public void processPayment(Payment payment) {
        try {
            // Process...
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            throw e;
        }
    }
}
```

#### Using Gauge for Queue Monitoring

```java
import io.micrometer.core.instrument.Gauge;

@Service
public class QueueMonitor {
    private final BlockingQueue<Task> taskQueue;
    
    public QueueMonitor(MeterRegistry registry, BlockingQueue<Task> queue) {
        this.taskQueue = queue;
        
        Gauge.builder("task.queue.depth", queue, Queue::size)
            .description("Number of tasks in processing queue")
            .register(registry);
    }
}
```

### Prometheus Endpoint

**Access metrics:** `http://localhost:8080/actuator/prometheus`

**Example Output:**
```prometheus
# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_bucket{method="GET",uri="/api/sample",status="200",le="0.001"} 45
http_server_requests_seconds_bucket{method="GET",uri="/api/sample",status="200",le="0.005"} 98
http_server_requests_seconds_bucket{method="GET",uri="/api/sample",status="200",le="0.01"} 120
http_server_requests_seconds_bucket{method="GET",uri="/api/sample",status="200",le="+Inf"} 150
http_server_requests_seconds_count{method="GET",uri="/api/sample",status="200"} 150
http_server_requests_seconds_sum{method="GET",uri="/api/sample",status="200"} 0.875

# Custom business metric
order_processing_seconds_count{status="success"} 1234
order_processing_seconds_sum{status="success"} 45.6
```

### Grafana Dashboard Setup

**Import Dashboard JSON** (see `grafana-dashboard.json` in repo)

**Key Panels:**

1. **Request Rate:** `rate(http_server_requests_seconds_count[1m])`
2. **Error Rate:** `rate(http_server_requests_seconds_count{status=~"5.."}[1m])`
3. **Latency p95:** `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))`
4. **JVM Memory:** `jvm_memory_used_bytes{area="heap"}`
5. **Active DB Connections:** `hikaricp_connections_active`

### Alert Rule Examples

**Prometheus AlertManager Rules:**

```yaml
groups:
  - name: service_alerts
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 2m
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors/sec"
      
      # Slow requests
      - alert: SlowRequests
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        annotations:
          summary: "P95 latency exceeds 1 second"
      
      # Database connection pool exhaustion
      - alert: ConnectionPoolExhausted
        expr: hikaricp_connections_pending > 0
        for: 1m
        annotations:
          summary: "Connection pool exhausted"
```

---

## 6. Health Checks & Probes

### Three Types of Health Endpoints

| Endpoint | Purpose | Kubernetes Probe | Checks |
|----------|---------|------------------|--------|
| `/actuator/health/readiness` | Is service ready to accept traffic? | Readiness Probe | DB, Kafka/RabbitMQ, Disk |
| `/actuator/health/liveness` | Is service alive and not deadlocked? | Liveness Probe | Application state only |
| `/actuator/health/startup` | Has service finished initialization? | Startup Probe | DB pool, messaging |

### Kubernetes Probe Configuration

**Deployment YAML:**
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: sample-service
        image: sample-service:1.0.0
        
        # Startup probe (slow-starting services)
        startupProbe:
          httpGet:
            path: /actuator/health/startup
            port: 8080
          failureThreshold: 30
          periodSeconds: 10
        
        # Readiness probe (traffic routing)
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Liveness probe (restart if dead)
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
```

### Built-in Health Indicators

#### Database Health Indicator

**Checks:** Database connectivity via validation query

**Response (UP):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "responsive",
        "validationQuery": "SELECT 1 FROM DUAL",
        "responseTimeMs": 15
      }
    }
  }
}
```

**Response (DOWN):**
```json
{
  "status": "DOWN",
  "components": {
    "db": {
      "status": "DOWN",
      "details": {
        "database": "unreachable",
        "error": "JdbcSQLException",
        "message": "Connection timeout"
      }
    }
  }
}
```

#### Kafka Health Indicator

**Checks:** Kafka broker connectivity (only when provider=kafka)

**Response:**
```json
{
  "status": "UP",
  "components": {
    "kafka": {
      "status": "UP",
      "details": {
        "kafka": "connected",
        "clusterId": "abc-123-xyz",
        "nodes": 3,
        "responseTimeMs": 45
      }
    }
  }
}
```

#### RabbitMQ Health Indicator

**Checks:** RabbitMQ broker connectivity (only when provider=rabbitmq)

### Creating Custom Health Indicators

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApiHealthIndicator implements HealthIndicator {
    
    private final RestTemplate restTemplate;
    private final String apiUrl = "https://external-api.com/health";
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            
            long latency = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("externalApi", "reachable")
                    .withDetail("latencyMs", latency)
                    .build();
            } else {
                return Health.down()
                    .withDetail("externalApi", "unhealthy")
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("externalApi", "unreachable")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Health Check Response Format

**Healthy Service:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": {...} },
    "kafka": { "status": "UP", "details": {...} },
    "diskSpace": { "status": "UP", "details": {...} },
    "ping": { "status": "UP" }
  }
}
```

**Unhealthy Service (503 status code):**
```json
{
  "status": "DOWN",
  "components": {
    "db": { "status": "DOWN", "details": {...} },
    "kafka": { "status": "UP", "details": {...} }
  }
}
```

### Troubleshooting Unhealthy Services

1. **Check individual components:**
   ```bash
   curl http://localhost:8080/actuator/health | jq '.components'
   ```

2. **Review component details:**
   ```bash
   curl http://localhost:8080/actuator/health | jq '.components.db.details'
   ```

3. **Check logs for health check errors:**
   ```bash
   kubectl logs pod-name | grep HealthIndicator
   ```

4. **Verify network connectivity:**
   ```bash
   kubectl exec -it pod-name -- nc -zv database-host 1521
   ```

---

## 7. Configuration Reference

### Complete Property Reference

#### Platform Observability Properties

**Prefix:** `platform.observability`

```yaml
platform:
  observability:
    # Distributed Tracing
    tracing:
      enabled: true                    # Enable/disable tracing
      sampling-rate: 1.0               # Sampling probability (0.0-1.0)
      service-name: ${spring.application.name}  # Service name in traces
      environment: ${spring.profiles.active}    # Environment tag
    
    # Metrics
    metrics:
      enabled: true                    # Enable/disable metrics
      jvm-metrics-enabled: true        # JVM memory, GC, threads
      http-metrics-enabled: true       # HTTP request metrics
      db-metrics-enabled: true         # Database metrics
      eventing-metrics-enabled: true   # Kafka/RabbitMQ metrics
    
    # Logging
    logging:
      request-logging-enabled: false   # HTTP request/response logging
      request-logging-include-headers: false   # Log HTTP headers
      request-logging-include-payload: false   # Log request/response bodies
      request-logging-excluded-paths:          # Paths to exclude from logging
        - /actuator/health
        - /actuator/prometheus
    
    # Health Checks
    health:
      timeout: 5s                      # Health check timeout
      db-check-enabled: true           # Enable database health check
      eventing-check-enabled: true     # Enable messaging health check
```

#### Spring Actuator Properties

**Prefix:** `management`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # Exposed endpoints
      base-path: /actuator                       # Base path for all endpoints
  
  endpoint:
    health:
      probes:
        enabled: true                  # Enable /health/readiness and /health/liveness
      show-details: when-authorized    # When to show health details (always|never|when-authorized)
      group:
        readiness:                     # Readiness probe configuration
          include: db,kafka,rabbitmq,diskSpace
          show-details: always
        liveness:                      # Liveness probe configuration
          include: ping,diskSpace
          show-details: always
  
  metrics:
    tags:                              # Common tags for all metrics
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
    distribution:
      percentiles-histogram:           # Enable histogram buckets
        http.server.requests: true
      percentiles:                     # Percentile values to calculate
        http.server.requests: 0.5,0.95,0.99
    enable:                            # Enable specific metric types
      jvm: true
      process: true
      system: true
      tomcat: true
      hikaricp: true
  
  health:
    defaults:
      enabled: true                    # Enable default health indicators
    db:
      enabled: true                    # Database health indicator
    diskspace:
      enabled: true                    # Disk space health indicator
  
  tracing:
    sampling:
      probability: 1.0                 # Sampling rate (0.0-1.0)
    baggage:
      enabled: true                    # Enable baggage propagation
      correlation:
        enabled: true                  # Enable correlation ID in baggage
        fields: correlationId          # Fields to propagate as baggage
  
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces  # OpenTelemetry collector endpoint
      timeout: 10s                               # Export timeout
      compression: gzip                          # Compression (none|gzip)
```

### Environment-Specific Configuration

**application-dev.yml:**
```yaml
platform:
  observability:
    tracing:
      sampling-rate: 1.0  # 100% sampling for development
    logging:
      request-logging-enabled: true  # Enable request logging for debugging

logging:
  level:
    com.company: DEBUG
    org.springframework.web: DEBUG
```

**application-prod.yml:**
```yaml
platform:
  observability:
    tracing:
      sampling-rate: 0.1  # 10% sampling for production (reduce overhead)
    logging:
      request-logging-enabled: false  # Disable request logging in production

logging:
  level:
    com.company: INFO
    org.springframework.web: WARN
```

### Environment Variables

Override via environment variables:

```bash
# OpenTelemetry endpoint
export OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces

# Service name
export SPRING_APPLICATION_NAME=my-service

# Sampling rate
export PLATFORM_OBSERVABILITY_TRACING_SAMPLING_RATE=0.1

# Enable request logging
export PLATFORM_OBSERVABILITY_LOGGING_REQUEST_LOGGING_ENABLED=true
```

---

## 8. Common Patterns & Recipes

### Pattern 1: Async Operation Tracing

**Problem:** Async methods lose trace context

**Solution:** MDC and trace context automatically propagated

```java
import org.springframework.scheduling.annotation.Async;
import io.micrometer.tracing.annotation.WithSpan;

@Service
public class NotificationService {
    
    @Async  // Runs in separate thread
    @WithSpan("send-notification")  // Creates child span
    public CompletableFuture<Void> sendNotification(String userId, String message) {
        // MDC and trace context automatically available here!
        log.info("Sending notification to user: {}", userId);
        // correlationId, traceId, spanId all present in logs
        
        // Send notification...
        
        return CompletableFuture.completedFuture(null);
    }
}
```

**Verification:**
```bash
# Logs show same correlationId and traceId in both threads
[main-thread] correlationId=abc-123, traceId=T1, spanId=S1, message="Async call initiated"
[async-thread] correlationId=abc-123, traceId=T1, spanId=S2, message="Sending notification"
```

### Pattern 2: Event-Driven Tracing

**Publishing Events with Trace Context:**

```java
@Service
public class OrderService {
    private final EventBus eventBus;
    
    public void createOrder(Order order) {
        // Trace context automatically added to event headers
        eventBus.publish(new EventMessage(
            "orders.created",
            order.getId(),
            serializeOrder(order),
            Map.of("orderType", "standard")
        ));
        
        log.info("Order created event published");
    }
}
```

**Consuming Events (Trace Context Extracted):**

```java
@Service
public class OrderEventConsumer {
    
    @KafkaListener(topics = "orders.created", groupId = "inventory-service")
    @WithSpan("process-order-created")  // Child span created
    public void handleOrderCreated(EventMessage message) {
        // Runs in same trace as publisher!
        // traceId matches the original request
        
        log.info("Processing order created event");
        // correlationId and traceId from publisher available in logs
        
        // Update inventory...
    }
}
```

**Jaeger Trace Shows:**
```
OrderService.createOrder [100ms]
  ├─ kafka.send orders.created [10ms]
  └─ OrderEventConsumer.handleOrderCreated [50ms]  ← Same trace!
      └─ database.update inventory [30ms]
```

### Pattern 3: Database Operation Monitoring

**Auto-Instrumented Database Queries:**

```java
@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;
    
    // Automatically traced and metered!
    public Order findById(String orderId) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            new Object[]{orderId},
            new OrderRowMapper()
        );
    }
}
```

**Metrics Generated:**
- `hikaricp_connections_active` - Active connections
- `jdbc_connections_max` - Max pool size
- Query execution time (in trace spans)

**Custom Metrics for Slow Queries:**

```java
@Repository
public class OrderRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Timer queryTimer;
    
    public OrderRepository(JdbcTemplate jdbcTemplate, MeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryTimer = Timer.builder("database.query.orders")
            .description("Time to query orders")
            .tag("table", "orders")
            .register(registry);
    }
    
    public List<Order> findByStatus(String status) {
        return queryTimer.record(() -> {
            return jdbcTemplate.query(
                "SELECT * FROM orders WHERE status = ?",
                new Object[]{status},
                new OrderRowMapper()
            );
        });
    }
}
```

### Pattern 4: External API Integration

**Adding Correlation ID to Outbound Requests:**

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.slf4j.MDC;

@Service
public class ExternalApiClient {
    private final RestTemplate restTemplate;
    
    public PaymentResponse processPayment(PaymentRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        // Forward correlation ID to external service
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.add("X-Correlation-ID", correlationId);
        }
        
        // Trace context automatically propagated via traceparent header
        
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(
            "https://payment-service.com/api/payments",
            entity,
            PaymentResponse.class
        );
    }
}
```

**Metrics for External API Calls:**

```java
@Service
public class ExternalApiClient {
    private final RestTemplate restTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer latencyTimer;
    
    public ExternalApiClient(RestTemplate restTemplate, MeterRegistry registry) {
        this.restTemplate = restTemplate;
        
        this.successCounter = Counter.builder("external.api.calls")
            .tag("status", "success")
            .tag("api", "payment-service")
            .register(registry);
            
        this.failureCounter = Counter.builder("external.api.calls")
            .tag("status", "failure")
            .tag("api", "payment-service")
            .register(registry);
            
        this.latencyTimer = Timer.builder("external.api.latency")
            .tag("api", "payment-service")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
    
    public PaymentResponse processPayment(PaymentRequest request) {
        return latencyTimer.record(() -> {
            try {
                PaymentResponse response = makeApiCall(request);
                successCounter.increment();
                return response;
            } catch (Exception e) {
                failureCounter.increment();
                throw e;
            }
        });
    }
}
```

### Pattern 5: Multi-Tenant Logging

**Adding Tenant Context to Logs:**

```java
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            // Extract tenant ID from header or JWT
            String tenantId = request.getHeader("X-Tenant-ID");
            
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.remove("tenantId");
        }
    }
}
```

**All logs now include tenant ID:**
```json
{
  "timestamp": "2026-04-20T10:30:45.123Z",
  "message": "Processing order",
  "correlationId": "abc-123",
  "tenantId": "customer-xyz",  ← Tenant context
  "userId": "john.doe"
}
```

**Filtering Logs by Tenant (Elasticsearch):**
```json
GET /logs-*/_search
{
  "query": {
    "term": { "tenantId": "customer-xyz" }
  }
}
```

**Tenant-Specific Metrics:**
```java
@Service
public class OrderService {
    private final MeterRegistry registry;
    
    public void processOrder(Order order) {
        String tenantId = MDC.get("tenantId");
        
        Counter.builder("orders.processed")
            .tag("tenantId", tenantId)
            .register(registry)
            .increment();
    }
}
```

---

## 9. Troubleshooting Guide

### Issue 1: Correlation ID Not Appearing in Logs

**Symptoms:**
- Logs missing `correlationId` field
- JSON logs present but MDC fields empty

**Diagnosis:**
```bash
# Check if CorrelationIdFilter is registered
curl http://localhost:8080/actuator/beans | jq '.contexts[].beans | to_entries[] | select(.key | contains("CorrelationIdFilter"))'

# Check logback configuration
ls -la src/main/resources/logback-spring.xml

# Test correlation ID manually
curl -H "X-Correlation-ID: test-123" http://localhost:8080/api/sample
# Check logs for "test-123"
```

**Solutions:**

1. **Verify logback-spring.xml exists:**
   ```bash
   # Should be in platform-starters/src/main/resources/
   ls platform-starters/src/main/resources/logback-spring.xml
   ```

2. **Verify MDC integration in CorrelationIdFilter:**
   ```java
   // Should have this line:
   MDC.put("correlationId", correlationId);
   ```

3. **Check filter ordering:**
   ```java
   // CorrelationIdFilter must run before logging
   @Bean
   @Order(Ordered.HIGHEST_PRECEDENCE)
   public CorrelationIdFilter correlationIdFilter() {
       return new CorrelationIdFilter();
   }
   ```

4. **Verify logback encoder dependency:**
   ```xml
   <dependency>
       <groupId>net.logstash.logback</groupId>
       <artifactId>logstash-logback-encoder</artifactId>
       <version>7.4</version>
   </dependency>
   ```

### Issue 2: Traces Not Appearing in Jaeger

**Symptoms:**
- No traces in Jaeger UI
- Spans not created
- `traceId` missing from logs

**Diagnosis:**
```bash
# Check if OTel exporter endpoint is reachable
curl http://localhost:4318/v1/traces

# Check application logs for OTel errors
kubectl logs pod-name | grep -i "opentelemetry\|otlp"

# Verify tracing is enabled
curl http://localhost:8080/actuator/configprops | jq '.["management.tracing-org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties"]'
```

**Solutions:**

1. **Verify OTLP endpoint configuration:**
   ```yaml
   management:
     otlp:
       tracing:
         endpoint: http://localhost:4318/v1/traces  # Must be reachable
   ```

2. **Check network connectivity:**
   ```bash
   # From application pod
   kubectl exec -it pod-name -- curl -v http://otel-collector:4318/v1/traces
   ```

3. **Increase sampling rate:**
   ```yaml
   platform:
     observability:
       tracing:
         sampling-rate: 1.0  # 100% for testing
   ```

4. **Verify dependencies:**
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-tracing-bridge-otel</artifactId>
   </dependency>
   <dependency>
       <groupId>io.opentelemetry</groupId>
       <artifactId>opentelemetry-exporter-otlp</artifactId>
   </dependency>
   ```

5. **Check Jaeger is running:**
   ```bash
   # Docker
   docker run -d --name jaeger \
     -p 16686:16686 \
     -p 4318:4318 \
     jaegertracing/all-in-one:latest
   ```

### Issue 3: Missing Metrics in Prometheus

**Symptoms:**
- `/actuator/prometheus` returns empty or missing metrics
- Specific metrics not appearing

**Diagnosis:**
```bash
# Check if prometheus endpoint is exposed
curl http://localhost:8080/actuator/prometheus | grep http_server_requests

# Check actuator configuration
curl http://localhost:8080/actuator/health | jq .

# Verify metrics are registered
curl http://localhost:8080/actuator/metrics | jq .
```

**Solutions:**

1. **Verify endpoint exposure:**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,prometheus  # Must include 'prometheus'
   ```

2. **Check Micrometer dependency:**
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

3. **Verify TimedAspect for @Timed annotations:**
   ```java
   @Bean
   public TimedAspect timedAspect(MeterRegistry registry) {
       return new TimedAspect(registry);
   }
   ```

4. **Check meter registration:**
   ```java
   // Metrics should be registered with MeterRegistry
   Counter counter = registry.counter("my.metric");
   counter.increment();  // IMPORTANT: Must be called to appear
   ```

### Issue 4: Health Check Always DOWN

**Symptoms:**
- `/actuator/health/readiness` returns `DOWN`
- Kubernetes pod not receiving traffic
- Specific health indicator failing

**Diagnosis:**
```bash
# Check individual health indicators
curl http://localhost:8080/actuator/health | jq '.components'

# Check specific component details
curl http://localhost:8080/actuator/health | jq '.components.db.details'

# Review health indicator logs
kubectl logs pod-name | grep -i "health"
```

**Solutions:**

1. **Database connectivity:**
   ```bash
   # Test database connection from pod
   kubectl exec -it pod-name -- nc -zv database-host 1521
   
   # Check JDBC URL configuration
   kubectl exec -it pod-name -- env | grep SPRING_DATASOURCE
   ```

2. **Kafka connectivity:**
   ```bash
   # Test Kafka broker connection
   kubectl exec -it pod-name -- nc -zv kafka-broker 9092
   
   # Check Kafka configuration
   kubectl exec -it pod-name -- env | grep KAFKA
   ```

3. **Increase health check timeout:**
   ```yaml
   platform:
     observability:
       health:
         timeout: 10s  # Increase from default 5s
   ```

4. **Disable problematic health indicators temporarily:**
   ```yaml
   management:
     health:
       kafka:
         enabled: false  # Disable if Kafka not required for readiness
   ```

5. **Check health indicator implementation:**
   ```java
   // Ensure health() method doesn't throw exceptions
   @Override
   public Health health() {
       try {
           // Check logic...
           return Health.up().build();
       } catch (Exception e) {
           return Health.down()
               .withDetail("error", e.getMessage())
               .build();
       }
   }
   ```

### Issue 5: High Memory Usage from Tracing

**Symptoms:**
- JVM heap growing continuously
- OutOfMemoryError
- High GC pressure

**Diagnosis:**
```bash
# Check heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq .

# Check for span leaks (spans not ended)
# Review application logs for span creation without corresponding end()
```

**Solutions:**

1. **Reduce sampling rate:**
   ```yaml
   platform:
     observability:
       tracing:
         sampling-rate: 0.1  # Only trace 10% of requests
   ```

2. **Ensure spans are properly closed:**
   ```java
   // BAD: Span never ended
   Span span = tracer.nextSpan().name("operation").start();
   doWork();  // If exception thrown, span leaks!
   
   // GOOD: Use try-with-resources
   Span span = tracer.nextSpan().name("operation").start();
   try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
       doWork();
   } finally {
       span.end();  // Always ended
   }
   ```

3. **Increase heap size:**
   ```yaml
   # Kubernetes deployment
   resources:
     requests:
       memory: "512Mi"
     limits:
       memory: "1Gi"
   env:
     - name: JAVA_OPTS
       value: "-Xmx768m -Xms512m"
   ```

4. **Monitor for span leaks:**
   ```java
   // Add custom metric to track active spans
   @Bean
   public MeterBinder spanTrackingBinder(Tracer tracer) {
       return registry -> {
           Gauge.builder("tracing.spans.active", tracer, t -> getActiveSpanCount())
               .register(registry);
       };
   }
   ```

### Issue 6: Async Operations Losing Correlation Context

**Symptoms:**
- Async method logs missing `correlationId`
- Different `correlationId` in async logs than parent request

**Diagnosis:**
```bash
# Check if MdcTaskDecorator is registered
curl http://localhost:8080/actuator/beans | jq '.contexts[].beans | to_entries[] | select(.key | contains("TaskExecutor"))'

# Check async logs
kubectl logs pod-name | grep -A5 "Async"
```

**Solutions:**

1. **Verify MdcTaskDecorator is configured:**
   ```java
   @Bean
   public Executor taskExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setTaskDecorator(new MdcTaskDecorator());  // REQUIRED
       executor.initialize();
       return executor;
   }
   ```

2. **Ensure @EnableAsync is present:**
   ```java
   @SpringBootApplication
   @EnableAsync  // Required for @Async support
   public class Application {
       public static void main(String[] args) {
           SpringApplication.run(Application.class, args);
       }
   }
   ```

3. **Use Spring's @Async instead of manual threads:**
   ```java
   // BAD: Manual thread creation loses MDC
   new Thread(() -> doWork()).start();
   
   // GOOD: Use @Async
   @Async
   public CompletableFuture<Void> doWork() {
       // MDC automatically propagated
   }
   ```

---

## 10. Migration Guide

### Migrating Existing Services to This Template

Follow these steps to adopt observability features in existing services:

#### Step 1: Add Platform-Starters Dependency

**pom.xml:**
```xml
<dependency>
    <groupId>com.company.platform</groupId>
    <artifactId>platform-starters</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Step 2: Update application.yml

Add observability configuration from [Configuration Reference](#7-configuration-reference).

**Minimal configuration:**
```yaml
platform:
  observability:
    tracing:
      service-name: ${spring.application.name}
      environment: ${spring.profiles.active}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

#### Step 3: Remove Custom Correlation/Logging Code

**Remove these if present:**
- Custom correlation ID filters (replaced by `CorrelationIdFilter`)
- Manual MDC management (auto-configured)
- Custom health check implementations (replaced by platform health indicators)

**Before:**
```java
// Remove custom correlation filter
@Component
public class CustomCorrelationFilter extends OncePerRequestFilter {
    // ...delete this class
}

// Remove manual MDC setup
public void processRequest() {
    MDC.put("requestId", UUID.randomUUID().toString());  // No longer needed
    // ...
    MDC.clear();
}
```

**After:**
```java
// Correlation ID automatically managed
public void processRequest() {
    log.info("Processing request");  // correlationId already in MDC
}
```

#### Step 4: Test Correlation ID Propagation

```bash
# Send request with correlation ID
curl -H "X-Correlation-ID: migration-test-123" \
     http://localhost:8080/api/endpoint

# Verify in logs
kubectl logs pod-name | grep "migration-test-123"

# Should see JSON logs with correlationId field
```

#### Step 5: Configure OpenTelemetry Exporter

Set OTLP endpoint via environment variable:

```yaml
# Kubernetes deployment
env:
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://otel-collector:4318/v1/traces"
```

Or in application.yml:

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

#### Step 6: Add Metrics to Critical Paths

Add `@Timed` annotations to important methods:

```java
import io.micrometer.core.annotation.Timed;

@Service
public class OrderService {
    
    @Timed(value = "order.create", percentiles = {0.95, 0.99})
    public Order createOrder(OrderRequest request) {
        // Existing logic...
    }
}
```

#### Step 7: Validate Health Checks

```bash
# Test readiness probe
curl http://localhost:8080/actuator/health/readiness

# Test liveness probe
curl http://localhost:8080/actuator/health/liveness

# Verify all components UP
curl http://localhost:8080/actuator/health | jq '.components'
```

#### Step 8: Update Kubernetes Manifests

Add health probes to deployment:

```yaml
spec:
  containers:
  - name: my-service
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
```

### Breaking Changes & Compatibility Notes

| Change | Impact | Migration Action |
|--------|--------|------------------|
| Logback format changed to JSON | Log parsers may break | Update log parsing (Logstash, Fluentd) |
| New MDC fields added | Existing MDC usage may conflict | Review custom MDC keys, avoid: correlationId, traceId, spanId |
| Health check endpoints changed | Old `/health` may behave differently | Use `/health/readiness` and `/health/liveness` explicitly |
| Async executor auto-configured | Custom async configs may conflict | Disable auto-config if needed: `@EnableAsync(mode = AdviceMode.ASPECTJ)` |

### Rollback Plan

If issues arise, rollback by:

1. **Remove platform-starters dependency:**
   ```xml
   <!-- Comment out in pom.xml -->
   <!-- <dependency>platform-starters</dependency> -->
   ```

2. **Restore old application.yml:**
   ```bash
   git checkout HEAD~1 -- src/main/resources/application.yml
   ```

3. **Redeploy previous version:**
   ```bash
   kubectl rollout undo deployment/my-service
   ```

---

## 11. Integration Examples

### Grafana Dashboard Setup

#### Step 1: Add Prometheus Data Source

1. Navigate to **Configuration** → **Data Sources**
2. Click **Add data source** → **Prometheus**
3. Set URL: `http://prometheus:9090`
4. Click **Save & Test**

#### Step 2: Import Dashboard

**Option A: Import JSON**

Use the provided `grafana-dashboard.json` from the repository:

```bash
# Download dashboard JSON
curl -o dashboard.json https://github.com/company/service-template/blob/main/observability/grafana-dashboard.json

# Import via UI
Grafana → Dashboards → Import → Upload JSON file
```

**Option B: Create Manually**

Key panels to create:

**1. Request Rate (QPS):**
```promql
sum(rate(http_server_requests_seconds_count{application="sample-service"}[1m]))
```

**2. Error Rate:**
```promql
sum(rate(http_server_requests_seconds_count{application="sample-service",status=~"5.."}[1m]))
/ sum(rate(http_server_requests_seconds_count{application="sample-service"}[1m]))
* 100
```

**3. P95 Latency:**
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket{application="sample-service"}[1m])) by (le, uri)
)
```

**4. JVM Memory:**
```promql
jvm_memory_used_bytes{application="sample-service",area="heap"}
/ jvm_memory_max_bytes{application="sample-service",area="heap"}
* 100
```

**5. Active Database Connections:**
```promql
hikaricp_connections_active{application="sample-service"}
```

**6. Kafka Consumer Lag:**
```promql
kafka_consumer_lag{application="sample-service"}
```

#### Dashboard Variables

Create template variables for dynamic filtering:

- **$application:** `label_values(http_server_requests_seconds_count, application)`
- **$environment:** `label_values(http_server_requests_seconds_count{application="$application"}, environment)`
- **$instance:** `label_values(http_server_requests_seconds_count{application="$application"}, instance)`

### Jaeger Setup

#### Option 1: Jaeger All-in-One (Development)

```bash
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

**Access UI:** http://localhost:16686

#### Option 2: Jaeger on Kubernetes (Production)

```bash
# Install Jaeger Operator
kubectl create namespace observability
kubectl create -f https://github.com/jaegertracing/jaeger-operator/releases/latest/download/jaeger-operator.yaml -n observability

# Deploy Jaeger instance
kubectl apply -f - <<EOF
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: jaeger-prod
  namespace: observability
spec:
  strategy: production
  storage:
    type: elasticsearch
    options:
      es:
        server-urls: http://elasticsearch:9200
  ingress:
    enabled: true
    hosts:
      - jaeger.company.com
EOF
```

**Configure application:**
```yaml
management:
  otlp:
    tracing:
      endpoint: http://jaeger-prod-collector:4318/v1/traces
```

#### Querying Traces in Jaeger

**1. Find trace by correlation ID:**
- Service: `sample-service`
- Tags: `correlationId=550e8400-e29b-41d4-a716-446655440000`
- Click **Find Traces**

**2. Find slow requests:**
- Service: `sample-service`
- Min Duration: `1s`
- Limit Results: `20`

**3. Find errors:**
- Service: `sample-service`
- Tags: `error=true`

### ELK Stack Integration

#### Logstash Pipeline Configuration

**logstash.conf:**
```ruby
input {
  tcp {
    port => 5000
    codec => json_lines
  }
}

filter {
  # Parse JSON logs
  json {
    source => "message"
    target => "log"
  }
  
  # Extract timestamp
  date {
    match => [ "[log][timestamp]", "ISO8601" ]
    target => "@timestamp"
  }
  
  # Add fields
  mutate {
    add_field => {
      "service" => "%{[log][service]}"
      "environment" => "%{[log][environment]}"
      "correlation_id" => "%{[log][correlationId]}"
      "trace_id" => "%{[log][traceId]}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{[service]}-%{+YYYY.MM.dd}"
  }
}
```

#### Elasticsearch Index Template

```json
PUT _index_template/logs-template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "logger": { "type": "keyword" },
        "message": { "type": "text" },
        "correlationId": { "type": "keyword" },
        "traceId": { "type": "keyword" },
        "spanId": { "type": "keyword" },
        "userId": { "type": "keyword" },
        "service": { "type": "keyword" },
        "environment": { "type": "keyword" }
      }
    }
  }
}
```

#### Kibana Dashboard Examples

**1. Correlation ID Drilldown:**

Create saved search:
```
correlationId: "550e8400-e29b-41d4-a716-446655440000"
| sort @timestamp asc
```

**2. Error Rate by Service:**

Visualization (TSVB):
```
Filter: level: "ERROR"
Group by: service.keyword
Aggregation: Count
```

**3. Top Loggers with Errors:**

Visualization (Data Table):
```
Filter: level: "ERROR"
Buckets: Terms aggregation on logger.keyword
Metric: Count
```

### Prometheus Setup

#### Prometheus Configuration

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-apps'
    metrics_path: '/actuator/prometheus'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - default
            - production
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: ([^:]+)(?::\\d+)?;(\\d+)
        replacement: $1:$2
      - source_labels: [__meta_kubernetes_pod_label_app]
        target_label: application
```

#### Kubernetes Pod Annotations

**deployment.yaml:**
```yaml
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
```

#### Recording Rules

**rules.yml:**
```yaml
groups:
  - name: application_rules
    interval: 30s
    rules:
      # Request rate (QPS)
      - record: application:http_requests:rate1m
        expr: sum(rate(http_server_requests_seconds_count[1m])) by (application)
      
      # Error rate (%)
      - record: application:http_errors:rate1m
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) by (application)
          / sum(rate(http_server_requests_seconds_count[1m])) by (application)
          * 100
      
      # P95 latency
      - record: application:http_latency:p95
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, application))
```

---

## 12. Best Practices & Guidelines

### Logging Best Practices

1. **Always use structured logging - never concatenate strings:**
   ```java
   // BAD
   log.info("Processing order " + orderId + " for user " + userId);
   
   // GOOD
   log.info("Processing order: {} for user: {}", orderId, userId);
   ```

2. **Use appropriate log levels:**
   - `ERROR`: Application errors requiring immediate attention
   - `WARN`: Unexpected conditions that don't prevent operation
   - `INFO`: Important business events (order created, payment processed)
   - `DEBUG`: Detailed diagnostic information
   - `TRACE`: Very detailed diagnostic information (rarely used)

3. **Don't log sensitive data:**
   ```java
   // BAD
   log.info("User password: {}", password);
   log.info("Credit card: {}", creditCard);
   
   // GOOD
   log.info("User authenticated: {}", userId);
   log.info("Payment processed: orderId={}", orderId);
   ```

4. **Add correlation IDs to all cross-service calls:**
   ```java
   String correlationId = MDC.get("correlationId");
   headers.add("X-Correlation-ID", correlationId);
   ```

### Tracing Best Practices

1. **Create custom spans for business-critical operations:**
   ```java
   @WithSpan("complex-business-logic")
   public void processComplexOperation() {
       // Span automatically created and timed
   }
   ```

2. **Add meaningful tags to spans:**
   ```java
   Span span = tracer.currentSpan();
   span.tag("order.id", orderId);
   span.tag("order.amount", String.valueOf(amount));
   span.tag("customer.tier", customerTier);
   ```

3. **Always close spans (use try-with-resources):**
   ```java
   Span span = tracer.nextSpan().name("operation").start();
   try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
       doWork();
   } finally {
       span.end();  // Critical!
   }
   ```

4. **Use sampling in production to reduce overhead:**
   ```yaml
   platform.observability.tracing.sampling-rate: 0.1  # 10%
   ```

### Metrics Best Practices

1. **Tag metrics with high-value dimensions only:**
   ```java
   // GOOD: Low-cardinality tags
   Counter.builder("orders.processed")
       .tag("status", "success")  // 2-3 values
       .tag("type", orderType)    // 5-10 values
       .register(registry);
   
   // BAD: High-cardinality tags (metrics explosion)
   Counter.builder("orders.processed")
       .tag("userId", userId)        // Thousands of values
       .tag("orderId", orderId)      // Unlimited values
       .register(registry);
   ```

2. **Use histograms for latency, not averages:**
   ```java
   // GOOD: Histogram with percentiles
   @Timed(value = "operation.duration", percentiles = {0.5, 0.95, 0.99})
   
   // BAD: Average latency (hides outliers)
   Timer timer = Timer.builder("operation.duration").register(registry);
   ```

3. **Monitor SLIs (Service Level Indicators):**
   - **Latency:** p95, p99 response times
   - **Error Rate:** 5xx responses / total requests
   - **Throughput:** Requests per second
   - **Saturation:** Connection pool usage, queue depth

4. **Set up alerts before going to production:**
   ```yaml
   # Example alert rules
   - alert: HighErrorRate
     expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
   - alert: HighLatency
     expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
   ```

### Health Check Best Practices

1. **Separate readiness and liveness concerns:**
   - **Readiness:** Checks dependencies (DB, Kafka) - can recover automatically
   - **Liveness:** Checks application state only - requires restart to recover

2. **Keep health checks fast (<1 second):**
   ```java
   // GOOD: Simple validation query
   SELECT 1 FROM DUAL
   
   // BAD: Heavy query
   SELECT COUNT(*) FROM large_table WHERE complex_condition
   ```

3. **Don't fail liveness for dependency issues:**
   ```java
   // Database failure should affect readiness, NOT liveness
   // Liveness: Is the application deadlocked? No → UP
   // Readiness: Can the application serve traffic? No (DB down) → DOWN
   ```

### Operational Guidelines

1. **Monitor observability system health:**
   - Prometheus scrape errors
   - Jaeger collector lag
   - Elasticsearch cluster health
   - Log ingestion rate

2. **Set retention policies:**
   - **Logs:** 30 days (hot), 90 days (cold storage)
   - **Metrics:** 15 days (raw), 1 year (aggregated)
   - **Traces:** 7 days

3. **Document custom metrics and spans:**
   - Maintain a metrics catalog
   - Document what each metric measures
   - Provide example queries

4. **Test observability in staging:**
   - Verify correlation IDs propagate end-to-end
   - Simulate failures, check alerts fire
   - Load test with tracing enabled

---

## 13. FAQ

### Q: Do I need to manually propagate correlation IDs?

**A:** No, correlation IDs are automatically propagated via:
- MDC for logging (within same JVM)
- `X-Correlation-ID` header for HTTP calls (you must forward it manually)
- Message headers for Kafka/RabbitMQ (platform-starters handles this)

### Q: How much overhead does tracing add?

**A:** With 10% sampling:
- Latency: <0.1% (<0.5ms average)
- CPU: <0.5%
- Memory: ~50MB additional heap

With 100% sampling (dev/test only):
- Latency: <1% (~0.5ms average)
- CPU: ~2%
- Memory: ~100MB additional heap

### Q: Can I use a different tracing backend (not Jaeger)?

**A:** Yes, OpenTelemetry supports multiple backends via OTLP protocol:
- **Jaeger:** `http://jaeger-collector:4318/v1/traces`
- **Tempo (Grafana):** `http://tempo:4318/v1/traces`
- **AWS X-Ray:** Use OTEL Collector with X-Ray exporter
- **Azure Monitor:** Use OTEL Collector with Azure exporter
- **Google Cloud Trace:** Use OTEL Collector with GCP exporter

Just change the `management.otlp.tracing.endpoint` configuration.

### Q: How do I view traces locally during development?

**A:** Run Jaeger all-in-one:
```bash
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Access UI: http://localhost:16686

### Q: What's the difference between logs and traces?

**A:**
- **Logs:** Individual events (text records) → "What happened?"
- **Traces:** Request journeys (spans across services) → "Where did time go?"

**Example:**
- Log: "Order 123 processed in 250ms"
- Trace: Shows 250ms breakdown: 50ms DB + 10ms Kafka + 100ms external API + 90ms business logic

Use **logs** for detailed events and debugging.  
Use **traces** for understanding request flow and identifying bottlenecks.

### Q: How do I reduce observability costs in production?

**A:**
1. **Reduce trace sampling:** `sampling-rate: 0.1` (10%)
2. **Shorten retention:** Logs 30d, traces 7d, metrics 15d
3. **Sample non-error logs:** Only 10% of 2xx requests
4. **Use cheaper storage:** S3/GCS for cold logs
5. **Filter noisy metrics:** Exclude high-cardinality tags

### Q: Can I disable observability features?

**A:** Yes, selectively disable features:

```yaml
platform:
  observability:
    tracing:
      enabled: false  # Disable tracing
    metrics:
      enabled: false  # Disable custom metrics
    logging:
      request-logging-enabled: false  # Disable request logging

management:
  endpoints:
    web:
      exposure:
        include: health  # Only expose health endpoint
```

### Q: How do I trace database queries?

**A:** Database queries are automatically traced by OpenTelemetry JDBC instrumentation. No code changes needed. Spans appear in Jaeger with SQL statement details.

**Example trace:**
```
GET /api/orders/123 [200ms]
  └─ SELECT * FROM orders WHERE id = ? [50ms]
```

### Q: How do I correlate logs across multiple services?

**A:**
1. Service A receives request with `X-Correlation-ID: abc-123`
2. Service A forwards header to Service B: `restTemplate.exchange(..., headers)`
3. Service B logs include `correlationId: "abc-123"`
4. Search logs: `correlationId: "abc-123"` → See all services involved

**Elasticsearch query:**
```json
GET /logs-*/_search
{
  "query": { "term": { "correlationId": "abc-123" }},
  "sort": [{ "timestamp": "asc" }]
}
```

### Q: What if my service doesn't use Spring Boot?

**A:** This template is Spring Boot-specific. For other frameworks:
- **Logging:** Use Logback/Log4j2 with JSON encoder
- **Tracing:** OpenTelemetry Java agent (framework-agnostic)
- **Metrics:** Micrometer or Prometheus client library
- **Health:** Implement HTTP `/health` endpoint manually

---

## 14. Additional Resources

### Official Documentation

- **Spring Boot Actuator:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer:** https://micrometer.io/docs
- **OpenTelemetry Java:** https://opentelemetry.io/docs/instrumentation/java/
- **Logback:** https://logback.qos.ch/manual/
- **Prometheus:** https://prometheus.io/docs/

### Tutorials & Guides

- **OpenTelemetry Getting Started:** https://opentelemetry.io/docs/instrumentation/java/getting-started/
- **Micrometer with Prometheus:** https://micrometer.io/docs/registry/prometheus
- **Spring Boot Distributed Tracing:** https://spring.io/blog/2022/10/12/observability-with-spring-boot-3

### Tools

- **Jaeger UI:** https://www.jaegertracing.io/docs/latest/getting-started/
- **Grafana:** https://grafana.com/docs/grafana/latest/
- **Kibana:** https://www.elastic.co/guide/en/kibana/current/index.html
- **Prometheus:** https://prometheus.io/docs/prometheus/latest/getting_started/

### Internal Resources

- **Platform Team Contacts:**
  - Slack: `#platform-observability`
  - Email: platform-team@company.com
  - On-call: PagerDuty rotation

- **Example Services:**
  - Reference implementation: `github.com/company/reference-service`
  - Production dashboard: `grafana.company.com/d/platform-services`

- **Runbooks:**
  - Observability incident response: `wiki.company.com/observability-incidents`
  - Alert runbooks: `github.com/company/runbooks/observability`

### Training

- **Observability 101 Workshop:** Monthly, register at `learning.company.com`
- **Advanced Tracing Techniques:** Quarterly deep-dive sessions
- **Metrics-Driven Development:** Self-paced course available

---

## Summary

You now have a **production-ready, enterprise-grade observability stack** with:

✅ **Structured JSON logging** with correlation IDs and MDC  
✅ **Distributed tracing** via OpenTelemetry  
✅ **Prometheus metrics** with custom business instrumentation  
✅ **Kubernetes-ready health probes**  
✅ **Auto-configured, zero-code observability** for most use cases

**Next Steps:**
1. Review the [Configuration Reference](#7-configuration-reference)
2. Try the [Common Patterns](#8-common-patterns--recipes)
3. Set up [Grafana](#grafana-dashboard-setup) and [Jaeger](#jaeger-setup)
4. Join `#platform-observability` on Slack for support

**Happy observing! 🔭📊📋**

