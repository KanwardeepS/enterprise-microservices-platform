# CLI Generator

**Author:** Kanwardeep Singh

A command-line tool that scaffolds a production-ready microservice by copying `service-template`, renaming Java packages, updating `application.yml`, and rewiring Maven coordinates — all in one command.

---

## How It Works

The generator:
1. Copies the entire `service-template` directory to your chosen output path (skipping `target/`)
2. Replaces all template placeholders (`${SERVICE_NAME}`, `com.company.service`, etc.) in Java, XML, YAML, and Markdown files
3. Rewrites `application.yml` to set your chosen event provider and database type
4. Moves the Java source tree from `com/company/service` to your declared package path

The result is a fully configured, ready-to-run Spring Boot service.

---

## Build

```bash
cd cli-generator
mvn clean package
```

This produces a shaded (self-contained) JAR at `target/cli-generator-1.0.0.jar` with all dependencies bundled.

---

## Usage

```bash
java -jar target/cli-generator-1.0.0.jar \
  --service-name <name> \
  --output-dir <path> \
  [options]
```

### Options

| Option | Required | Default | Description |
|---|---|---|---|
| `--service-name` | Yes | — | Human-readable name for the new service (e.g. `order-service`) |
| `--output-dir` | Yes | — | Directory where the generated service will be written |
| `--template-dir` | No | `service-template` | Path to the source template directory |
| `--group-id` | No | `com.company.services` | Maven `groupId` for the generated service |
| `--artifact-id` | No | sanitized service name | Maven `artifactId` (defaults to kebab-case of `--service-name`) |
| `--package-name` | No | groupId + service name | Java root package (e.g. `com.mycompany.orders`) |
| `--event-provider` | No | `kafka` | Event provider: `kafka` or `rabbitmq` |
| `--database` | No | `oracle` | Database type: `oracle` or `sybase` |
| `--security-enabled` | No | `true` | Whether to enable the platform security starter |

---

## Examples

### Minimal — defaults for everything optional
```bash
java -jar target/cli-generator-1.0.0.jar \
  --service-name order-service \
  --output-dir ../order-service
```

Generates a service at `../order-service` with:
- groupId: `com.company.services`
- artifactId: `order-service`
- package: `com.company.services.orderservice`
- Event provider: Kafka
- Database: Oracle
- Security: enabled

---

### Full — all options specified
```bash
java -jar target/cli-generator-1.0.0.jar \
  --service-name payment-service \
  --output-dir ../payment-service \
  --group-id com.mycompany.platform \
  --artifact-id payment-svc \
  --package-name com.mycompany.platform.payment \
  --event-provider rabbitmq \
  --database sybase \
  --security-enabled false
```

---

### RabbitMQ + Oracle service
```bash
java -jar target/cli-generator-1.0.0.jar \
  --service-name inventory-service \
  --output-dir ../inventory-service \
  --event-provider rabbitmq \
  --database oracle
```

---

## What Gets Generated

```
<output-dir>/
├── src/
│   └── main/
│       ├── java/<your-package>/
│       │   ├── Application.java
│       │   ├── config/
│       │   │   ├── OpenApiConfig.java
│       │   │   └── WebConfig.java
│       │   ├── controller/
│       │   │   ├── SampleController.java
│       │   │   └── OrderController.java    # full CRUD example
│       │   └── dto/
│       │       ├── CreateOrderRequest.java
│       │       └── OrderResponse.java
│       └── resources/
│           └── application.yml             # pre-configured for your stack
├── Dockerfile
└── pom.xml                                 # wired to platform-starters
```

The generated service:
- Compiles and runs immediately with `mvn spring-boot:run`
- Has Swagger UI at `http://localhost:8080/swagger-ui.html`
- Has health probes at `http://localhost:8080/actuator/health`
- Has Prometheus metrics at `http://localhost:8080/actuator/prometheus`

---

## Prerequisites

- Java 17+
- Maven 3.8+
- `platform-starters` must be installed to local Maven repo first:
  ```bash
  cd ../platform-starters && mvn clean install
  ```

---

## License

MIT License - Copyright © 2026 Kanwardeep Singh
