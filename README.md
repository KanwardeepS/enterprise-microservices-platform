# Enterprise Microservices Platform

**Author:** Kanwardeep Singh

A monorepo demonstrating production-grade patterns for building consistent, observable, and secure Java microservices with Spring Boot. This project serves as both a **reference implementation** and a **reusable platform library** that teams can adopt to fast-track new service development.

---

## Repository Structure

```
├── platform-starters/          # Reusable Spring Boot starter library
├── service-template/           # Production-ready microservice template
├── authentication-examples/    # Auth mechanism reference implementations
└── cli-generator/              # CLI tool to scaffold new services
```

---

## Modules

### `platform-starters`
A Spring Boot auto-configuration library that any service can pull in as a Maven dependency to get the full platform stack out of the box — no boilerplate required.

| Layer | What it provides |
|---|---|
| **API** | `ApiResponse<T>` wrapper, `ErrorResponse`, `GlobalExceptionHandler`, `PaginationMetadata` |
| **Logging** | JSON structured logs via Logback, correlation ID propagation (MDC), async MDC support |
| **Observability** | OpenTelemetry distributed tracing, Prometheus/Micrometer metrics, Kubernetes health probes |
| **Security** | JWT audience validation, `@RequireRoles` annotation (AOP-backed), OAuth2 resource server wiring |
| **Eventing** | `EventBus` abstraction with pluggable Kafka and RabbitMQ implementations |
| **Database** | HikariCP connection pool configuration with Oracle/Sybase support |

### `service-template`
A ready-to-run Spring Boot application that demonstrates how all platform features work together. Clone it as the starting point for any new service.

Key files:
- [Application.java](service-template/src/main/java/com/company/service/Application.java) — main entry point
- [OrderController.java](service-template/src/main/java/com/company/service/controller/OrderController.java) — full CRUD example with validation, pagination, and standard responses
- [application.yml](service-template/src/main/resources/application.yml) — all platform knobs with documented defaults

Deep-dive docs inside the module:
- [OBSERVABILITY.md](service-template/OBSERVABILITY.md) — logging, tracing, metrics, health checks
- [API-STANDARDS.md](service-template/API-STANDARDS.md) — REST conventions, error handling, versioning
- [ADOPTION-GUIDE.md](service-template/ADOPTION-GUIDE.md) — how to distribute and adopt this platform

### `authentication-examples`
Standalone, compilable reference implementations for four authentication mechanisms. No framework magic — just clear, focused code you can copy and adapt.

| Mechanism | Files |
|---|---|
| **JWT** | `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtSecurityConfig`, `AuthController` |
| **OAuth2** | `OAuth2Config`, `OAuth2UserController` |
| **Kerberos / SPNEGO** | `KerberosSecurityConfig` |
| **SSL / TLS** | `KeyCertificateLoader` |

See [authentication-examples/README.md](authentication-examples/README.md) for dependency snippets and usage notes.

### `cli-generator`
A PicoCLI-based command-line tool that scaffolds a new service by copying `service-template`, renaming packages, and rewriting `application.yml` for the chosen event provider and database.

**Usage:**
```bash
# Build the shaded JAR
cd cli-generator
mvn clean package

# Generate a new service
java -jar target/cli-generator-*.jar \
  --service-name order-service \
  --output-dir ../order-service \
  --group-id com.mycompany.services \
  --event-provider kafka \
  --database oracle
```

Options:

| Option | Required | Default | Description |
|---|---|---|---|
| `--service-name` | Yes | — | Human-readable service name |
| `--output-dir` | Yes | — | Where to write the generated service |
| `--group-id` | No | `com.company.services` | Maven groupId |
| `--artifact-id` | No | sanitized service name | Maven artifactId |
| `--package-name` | No | groupId + service name | Java root package |
| `--event-provider` | No | `kafka` | `kafka` or `rabbitmq` |
| `--database` | No | `oracle` | `oracle` or `sybase` |
| `--security-enabled` | No | `true` | Toggle security starter |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (optional — for running Jaeger, Prometheus, Keycloak locally)

### Build All Modules
```bash
# Build platform-starters first (other modules depend on it)
cd platform-starters && mvn clean install
cd ../service-template && mvn clean install
cd ../authentication-examples && mvn clean install
cd ../cli-generator && mvn clean package
```

### Run the Service Template
```bash
cd service-template
mvn spring-boot:run
```

| Endpoint | URL |
|---|---|
| Health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` |

---

## Platform Features at a Glance

### Standard API Response
Every endpoint returns a consistent envelope — no per-controller wiring needed:
```json
{
  "data": { ... },
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "pagination": { "page": 1, "size": 20, "totalElements": 150 }
  }
}
```

### Structured JSON Logging
All log lines are machine-parseable and include correlation ID, trace ID, and service name automatically:
```json
{
  "timestamp": "2026-04-20T10:30:45.123Z",
  "level": "INFO",
  "service": "order-service",
  "correlationId": "550e8400-...",
  "traceId": "4bf92f3577b34da6...",
  "message": "Order created successfully"
}
```

### Role-Based Access Control
```java
@GetMapping("/admin/config")
@RequireRoles({"ADMIN", "PLATFORM_OPS"})
public ApiResponse<Config> getConfig() { ... }
```

### Event Publishing
```java
// Kafka or RabbitMQ — same interface, swapped via config
eventBus.publish(EventMessage.of("order.created", payload));
```

### Kubernetes Health Probes
```yaml
# Readiness: checks db, kafka, rabbitmq, diskSpace
# Liveness:  checks ping, diskSpace
management.endpoint.health.probes.enabled: true
```

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.1 |
| Build | Maven 3.8 (multi-module) |
| Auth | Spring Security, OAuth2, JJWT 0.11.5 |
| Messaging | Apache Kafka, RabbitMQ |
| Observability | OpenTelemetry, Micrometer, Prometheus, Logstash Logback |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Containerization | Docker (multi-stage build) |
| Orchestration | Kubernetes (health probes included) |
| CLI | PicoCLI 4.7.6 |
