# Service Template - Production-Ready Spring Boot Microservice

**Author:** Kanwardeep Singh
**Version:** 1.0.0  
**Last Updated:** April 2026

A production-ready Spring Boot service template with **built-in observability** and **standard API conventions** for rapid microservice development.

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (optional, for running Jaeger/Prometheus locally)

### Run Locally

```bash
# Build the project
mvn clean install

# Run the service
mvn spring-boot:run

# Service available at http://localhost:8080
```

### Test the API

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI (interactive API docs)
open http://localhost:8080/swagger-ui.html

# Create an order
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token" \
  -d '{
    "customerId": "CUST-12345",
    "items": [
      {"productId": "PROD-100", "quantity": 2}
    ]
  }'

# Get order
curl http://localhost:8080/api/v1/orders/ORD-abc123

# View metrics
curl http://localhost:8080/actuator/prometheus
```

---

## ✨ Key Features

### 🔭 Built-in Observability

| Feature | Description | Documentation |
|---------|-------------|---------------|
| **Structured Logging** | JSON logs with correlation IDs, MDC fields | [OBSERVABILITY.md](../docs/OBSERVABILITY.md#3-structured-logging-guide) |
| **Distributed Tracing** | OpenTelemetry with W3C trace context | [OBSERVABILITY.md](../docs/OBSERVABILITY.md#4-distributed-tracing-guide) |
| **Metrics** | Prometheus metrics with @Timed support | [OBSERVABILITY.md](../docs/OBSERVABILITY.md#5-metrics--monitoring-guide) |
| **Health Checks** | Kubernetes-ready probes (readiness/liveness) | [OBSERVABILITY.md](../docs/OBSERVABILITY.md#6-health-checks--probes) |
| **Correlation IDs** | Auto-generated UUIDs across all requests | [OBSERVABILITY.md](../docs/OBSERVABILITY.md#2-architecture-overview) |

**Zero configuration required** - just use `platform-starters` dependency!

### 📋 Standard API Conventions

| Feature | Description | Documentation |
|---------|-------------|---------------|
| **REST Standards** | Resource-oriented URLs, standard HTTP methods | [API-STANDARDS.md](../docs/API-STANDARDS.md#2-rest-api-conventions) |
| **Standard Responses** | Consistent ApiResponse wrapper with metadata | [API-STANDARDS.md](../docs/API-STANDARDS.md#3-standard-response-formats) |
| **Error Handling** | Global exception handler, standard error format | [API-STANDARDS.md](../docs/API-STANDARDS.md#4-error-handling--status-codes) |
| **Validation** | Bean Validation with detailed error messages | [API-STANDARDS.md](../docs/API-STANDARDS.md#5-request-validation) |
| **OpenAPI Docs** | Auto-generated Swagger UI | [API-STANDARDS.md](../docs/API-STANDARDS.md#8-openapi-documentation) |
| **Versioning** | URI-based versioning (/api/v1/) | [API-STANDARDS.md](../docs/API-STANDARDS.md#6-api-versioning) |

### 🔒 Security

- **OAuth2 JWT** authentication (Bearer tokens)
- **Role-based authorization** (@RequireRoles annotation)
- **Security headers** (CORS, CSP, etc.)
- **Audience validation** for JWT tokens

---

## 📁 Project Structure

```
service-template/
├── src/
│   └── main/
│       ├── java/com/company/service/
│       │   ├── Application.java              # Main application class
│       │   ├── config/
│       │   │   └── OpenApiConfig.java        # Swagger/OpenAPI configuration
│       │   ├── controller/
│       │   │   ├── SampleController.java     # Basic example controller
│       │   │   └── OrderController.java      # Full CRUD example with validation
│       │   └── dto/
│       │       ├── CreateOrderRequest.java   # Request DTO with validation
│       │       └── OrderResponse.java        # Response DTO
│       └── resources/
│           └── application.yml               # Configuration with observability settings
├── OBSERVABILITY.md                          # Comprehensive observability guide (8000+ words)
├── API-STANDARDS.md                          # API design standards (8000+ words)
├── Dockerfile                                # Docker image build
└── pom.xml                                   # Maven dependencies

platform-starters/
├── src/main/java/com/company/platform/
│   ├── api/
│   │   ├── ApiResponse.java                  # Standard response wrapper
│   │   ├── ErrorResponse.java                # Standard error response
│   │   ├── PaginationMetadata.java           # Pagination info
│   │   ├── GlobalExceptionHandler.java       # Global error handling
│   │   ├── ResourceNotFoundException.java    # 404 exception
│   │   └── BusinessRuleViolationException.java # 422 exception
│   ├── logging/
│   │   ├── CorrelationIdFilter.java          # Correlation ID management
│   │   ├── MdcTaskDecorator.java             # Async MDC propagation
│   │   ├── RequestLoggingFilter.java         # HTTP request/response logging
│   │   └── LoggingAutoConfiguration.java     # Logging auto-config
│   ├── observability/
│   │   ├── DatabaseHealthIndicator.java      # DB health check
│   │   ├── KafkaHealthIndicator.java         # Kafka health check
│   │   ├── RabbitMqHealthIndicator.java      # RabbitMQ health check
│   │   ├── ObservabilityProperties.java      # Configuration properties
│   │   └── ObservabilityAutoConfiguration.java # Metrics, tracing setup
│   ├── security/
│   │   ├── JwtAudienceValidationFilter.java  # JWT audience validation
│   │   ├── RequireRoles.java                 # Role annotation
│   │   └── SecurityStarterAutoConfiguration.java
│   ├── eventing/
│   │   ├── EventBus.java                     # Event abstraction
│   │   ├── KafkaEventBus.java                # Kafka implementation
│   │   └── RabbitMqEventBus.java             # RabbitMQ implementation
│   └── db/
│       └── DbStarterAutoConfiguration.java   # Database auto-config
└── src/main/resources/
    ├── logback-spring.xml                    # JSON logging configuration
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 📚 Documentation

### For Developers

| Document | Purpose | Audience |
|----------|---------|----------|
| **[API-STANDARDS.md](../docs/API-STANDARDS.md)** | API design conventions, examples, best practices | All developers |
| **[OBSERVABILITY.md](../docs/OBSERVABILITY.md)** | Logging, tracing, metrics, health checks | Platform/DevOps engineers |
| **[ADOPTION-GUIDE.md](../docs/ADOPTION-GUIDE.md)** | Distribution strategy, adoption tracking, governance | Platform Engineering leaders |
| **README.md** (this file) | Quick start, architecture overview | New team members |

### Key Sections

#### API Standards
- ✅ REST conventions (URLs, methods, status codes)
- ✅ Standard response formats (success, error, pagination)
- ✅ Request validation patterns
- ✅ OpenAPI/Swagger documentation
- ✅ Complete CRUD examples

#### Observability
- ✅ Structured JSON logging with MDC
- ✅ Distributed tracing with OpenTelemetry
- ✅ Prometheus metrics integration
- ✅ Kubernetes health probes
- ✅ Correlation ID propagation
- ✅ Integration guides (Grafana, Jaeger, ELK)

---

## 🛠️ Configuration

### Application Properties

**Key configuration sections in `application.yml`:**

```yaml
# Observability
platform:
  observability:
    tracing:
      sampling-rate: 1.0              # 100% sampling (reduce in production)
    metrics:
      enabled: true
    logging:
      request-logging-enabled: false  # Disable in production

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true                 # Enable Kubernetes probes

# OpenTelemetry
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces

# Security
platform:
  security:
    jwt:
      audience: service-template
```

### Environment Variables

```bash
# Service name
export SPRING_APPLICATION_NAME=my-service

# OpenTelemetry endpoint
export OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces

# Trace sampling rate
export PLATFORM_OBSERVABILITY_TRACING_SAMPLING_RATE=0.1  # 10% in production

# Log level
export LOGGING_LEVEL_COM_COMPANY=INFO
```

---

## 🔍 Observability Stack (Local Development)

### Start Observability Tools

```bash
# Jaeger (tracing)
docker run -d --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest

# Prometheus (metrics)
docker run -d --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

# Grafana (dashboards)
docker run -d --name grafana \
  -p 3000:3000 \
  grafana/grafana
```

### Access UIs

| Tool | URL | Purpose |
|------|-----|---------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation |
| **Actuator** | http://localhost:8080/actuator | Health, metrics |
| **Jaeger** | http://localhost:16686 | Distributed traces |
| **Prometheus** | http://localhost:9090 | Metrics queries |
| **Grafana** | http://localhost:3000 | Dashboards |

---

## 🧪 Testing

### Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean verify jacoco:report
```

### API Testing Examples

**Test correlation ID propagation:**
```bash
# Send request with custom correlation ID
curl -H "X-Correlation-ID: test-abc-123" \
     http://localhost:8080/api/v1/orders

# Check logs - should see "test-abc-123" in all log entries
docker logs service-template | grep "test-abc-123"
```

**Test validation:**
```bash
# Invalid request (missing required fields)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"items": []}'

# Response: 400 Bad Request with detailed validation errors
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "customerId",
        "message": "Customer ID is required"
      },
      {
        "field": "items",
        "message": "At least one item is required"
      }
    ]
  }
}
```

---

## 🚢 Deployment

### Docker Build

```bash
# Build image
docker build -t service-template:1.0.0 .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318/v1/traces \
  service-template:1.0.0
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: service-template
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: service-template
        image: service-template:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        
        # Health probes
        startupProbe:
          httpGet:
            path: /actuator/health/startup
            port: 8080
          failureThreshold: 30
          periodSeconds: 10
        
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

---

## 📊 Monitoring & Alerts

### Key Metrics to Monitor

| Metric | Alert Threshold | Description |
|--------|----------------|-------------|
| `http_server_requests_seconds{status=~"5.."}` | >5% error rate | Server errors |
| `http_server_requests_seconds{quantile="0.95"}` | >1 second | P95 latency |
| `jvm_memory_used_bytes / jvm_memory_max_bytes` | >90% | Memory usage |
| `hikaricp_connections_pending` | >0 | DB connection pool exhaustion |

### Grafana Dashboards

Import pre-built dashboards:
- **JVM Dashboard:** ID 4701
- **Spring Boot Dashboard:** ID 11378
- **Custom Application Dashboard:** See `grafana-dashboard.json`

---

## 🤝 Contributing

### Development Workflow

1. **Create feature branch:** `git checkout -b feature/my-feature`
2. **Make changes** following API standards
3. **Add tests** for new functionality
4. **Run checks:** `mvn clean verify`
5. **Update documentation** if needed
6. **Submit pull request**

### Code Review Checklist

- ✅ Follows REST API conventions
- ✅ Uses standard ApiResponse wrapper
- ✅ Includes validation on request DTOs
- ✅ Has OpenAPI annotations
- ✅ Includes @Timed metrics on endpoints
- ✅ Structured logging (no string concatenation)
- ✅ Tests added/updated
- ✅ Documentation updated

---

## 📞 Support & Resources

### Internal Resources

- **Slack:** `#platform-engineering`
- **Wiki:** https://wiki.company.com/platform
- **API Review:** `#api-standards-review`

### Training

- **Observability 101:** Monthly workshop
- **API Design Masterclass:** Quarterly
- **Spring Boot Best Practices:** Self-paced course

### Reference Services

- **Example Service:** https://github.com/company/reference-service
- **Production Dashboard:** https://grafana.company.com/d/platform-services

---

## � Distribution & Adoption

### Making the Template Available to All Teams

This template can be distributed to all development teams through multiple channels:

#### 🚀 Quick Distribution Options

| Method | Use Case | Setup Time |
|--------|----------|------------|
| **GitHub Template** | Teams familiar with GitHub | 5 minutes |
| **Internal Maven Repository** | For platform-starters dependency | 30 minutes |
| **CLI Generator** | Automated service creation | 15 minutes |
| **Developer Portal** | Self-service via Backstage | 1 hour |

#### 📊 Tracking Adoption

Monitor template adoption across your organization:

**Automated Tracking Methods:**
1. **Maven Dependency Analysis** - Track teams using `platform-starters`
2. **GitHub Repository Metadata** - Count repos created from template
3. **Service Registry Tags** - Track via Consul/Eureka metadata
4. **Observability Metrics** - Count services with `template="service-template"` tag

**Key Metrics to Track:**
- Total services using template
- Adoption by team/organization
- Template version distribution
- New services created per month
- Support ticket volume

**Example Adoption Dashboard Query (Prometheus):**
```promql
# Total services using template
count(up{template="service-template"})

# Adoption by team
count by (team) (up{template="service-template"})
```

### 📚 Complete Distribution Guide

See **[ADOPTION-GUIDE.md](../docs/ADOPTION-GUIDE.md)** for comprehensive instructions on:

- ✅ Setting up distribution channels (Maven, GitHub, CLI)
- ✅ Onboarding new teams (self-service and guided)
- ✅ Implementing adoption tracking (automated and manual)
- ✅ Measuring success metrics and KPIs
- ✅ Governance model and support tiers
- ✅ Training materials and workshops

**Quick Links:**
- [Distribution Strategy](../docs/ADOPTION-GUIDE.md#1-distribution-strategy)
- [Adoption Tracking](../docs/ADOPTION-GUIDE.md#4-adoption-tracking)
- [Support Channels](../docs/ADOPTION-GUIDE.md#51-support-channels)
- [Success Metrics](../docs/ADOPTION-GUIDE.md#6-success-metrics)

---

## 📝 License

MIT License - Copyright © 2026 Kanwardeep Singh

---

## 🎯 What's Next?

### For Platform Engineering Teams

Want to roll this out organization-wide? See [ADOPTION-GUIDE.md](../docs/ADOPTION-GUIDE.md) for:
- Step-by-step distribution setup
- Automated adoption tracking dashboards
- Training programs and workshops
- Governance and support models

### For New Services

1. **Clone this template:** `git clone <repo-url>`
2. **Rename package:** `com.company.service` → `com.company.yourservice`
3. **Update application.yml:** Set `spring.application.name`
4. **Review [API-STANDARDS.md](../docs/API-STANDARDS.md)** for conventions
5. **Start building!** Controllers, services, repositories

### For Existing Services

See the [Migration Guide](../docs/OBSERVABILITY.md#10-migration-guide) in OBSERVABILITY.md for step-by-step instructions to adopt these features.

---

**Built with ❤️ by Platform Engineering Team**  
**Questions? Contact: platform-team@company.com**
