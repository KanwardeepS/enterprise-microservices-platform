# Platform Starters

**Author:** Kanwardeep Singh

A reusable Spring Boot auto-configuration library that gives any microservice a complete platform stack — structured logging, distributed tracing, Prometheus metrics, JWT security, role-based access control, and a pluggable event bus — with zero boilerplate.

---

## How to Use

Add `platform-starters` as a dependency in your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.company.platform</groupId>
    <artifactId>platform-starters</artifactId>
    <version>1.0.0</version>
</dependency>
```

All features auto-configure on startup. Override defaults in your `application.yml` as needed.

---

## What's Included

### API Layer
Consistent request/response contracts across all endpoints — no per-controller setup required.

| Class | Purpose |
|---|---|
| `ApiResponse<T>` | Standard response wrapper with data + metadata (correlationId, timestamp, pagination) |
| `ErrorResponse` | Standard error format with error code, message, and field-level validation details |
| `GlobalExceptionHandler` | Catches all exceptions and maps them to `ErrorResponse` with correct HTTP status |
| `ResourceNotFoundException` | Throws HTTP 404 with standard error body |
| `BusinessRuleViolationException` | Throws HTTP 422 with standard error body |
| `PaginationMetadata` | Pagination info (page, size, totalElements, totalPages) embedded in `ApiResponse` |

**Example response:**
```json
{
  "data": { "orderId": "ORD-abc123", "status": "CONFIRMED" },
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:30:45.123Z"
  }
}
```

---

### Logging
Structured JSON logging with automatic correlation ID propagation.

| Class | Purpose |
|---|---|
| `CorrelationIdFilter` | Generates or inherits `X-Correlation-ID` header; stores in MDC |
| `RequestLoggingFilter` | Logs HTTP method, path, status, duration, and body (configurable) |
| `MdcTaskDecorator` | Propagates MDC context (correlationId, traceId) into async threads |
| `LoggingAutoConfiguration` | Registers all logging filters automatically |

**Sample log output:**
```json
{
  "timestamp": "2026-04-20T10:30:45.123Z",
  "level": "INFO",
  "service": "order-service",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "message": "POST /api/v1/orders 201 45ms"
}
```

**Configuration:**
```yaml
platform:
  observability:
    logging:
      request-logging-enabled: true   # log every request/response
```

---

### Observability
Distributed tracing, Prometheus metrics, and Kubernetes health probes out of the box.

| Class | Purpose |
|---|---|
| `ObservabilityAutoConfiguration` | Wires OpenTelemetry tracing and Micrometer Prometheus registry |
| `ObservabilityProperties` | Binds `platform.observability.*` config properties |
| `DatabaseHealthIndicator` | Liveness/readiness check for the configured database |
| `KafkaHealthIndicator` | Readiness check for Kafka broker connectivity |
| `RabbitMqHealthIndicator` | Readiness check for RabbitMQ broker connectivity |

**Configuration:**
```yaml
platform:
  observability:
    tracing:
      enabled: true
      sampling-rate: 1.0       # 1.0 = 100% in dev; use 0.1 in prod
    metrics:
      enabled: true

management:
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true          # exposes /actuator/health/readiness and /liveness
```

---

### Security
JWT audience validation and annotation-driven role checks via AOP.

| Class | Purpose |
|---|---|
| `JwtAudienceValidationFilter` | Rejects JWT tokens that don't include the configured audience claim |
| `RequireRoles` | Annotation to declare required roles on a method or class |
| `RequireRolesAspect` | AOP aspect that enforces `@RequireRoles` — throws 403 if roles not met |
| `SecurityStarterAutoConfiguration` | Registers security filters and AOP aspect |
| `SecurityStarterProperties` | Binds `platform.security.*` config properties |

**Usage:**
```java
@GetMapping("/admin/config")
@RequireRoles({"ADMIN", "PLATFORM_OPS"})
public ApiResponse<Config> getAdminConfig() {
    return ApiResponse.success(configService.get());
}
```

**Configuration:**
```yaml
platform:
  security:
    enabled: true
    required-audience: platform-api   # must be present in JWT `aud` claim

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/platform
```

---

### Eventing
A provider-agnostic event bus with Kafka and RabbitMQ implementations, swapped via config.

| Class | Purpose |
|---|---|
| `EventBus` | Interface for publishing events |
| `EventMessage` | Event envelope: type, payload, metadata |
| `EventMessageHandler` | Interface for consuming events |
| `KafkaEventBus` | Kafka implementation of `EventBus` |
| `KafkaEventConsumer` | Kafka consumer wiring |
| `RabbitMqEventBus` | RabbitMQ implementation of `EventBus` |
| `RabbitMqEventConsumer` | RabbitMQ consumer wiring |
| `EventingAutoConfiguration` | Auto-selects implementation based on `platform.eventing.provider` |

**Usage:**
```java
@Autowired
private EventBus eventBus;

// Publish — same call regardless of Kafka or RabbitMQ
eventBus.publish(EventMessage.of("order.created", orderPayload));
```

**Configuration:**
```yaml
platform:
  eventing:
    provider: kafka          # or rabbitmq

    kafka:
      topic: platform.events
      consumer-group: order-service

    rabbitmq:
      exchange: platform.events
      queue: order-service.events
```

---

### Database
HikariCP connection pool wired from a single `platform.db.*` config block.

**Configuration:**
```yaml
platform:
  db:
    type: oracle             # or sybase
    pool:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout-ms: 30000
```

---

## Building

```bash
# Install to local Maven repository (required before dependent modules can build)
mvn clean install
```

---

## Module Dependencies

```xml
<!-- Spring Boot auto-configuration -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Observability -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Structured logging -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Messaging -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## License

MIT License - Copyright © 2026 Kanwardeep Singh
