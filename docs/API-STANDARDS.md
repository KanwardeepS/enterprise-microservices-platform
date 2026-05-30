# API Design Standards & Guidelines

**Version:** 1.0.0  
**Last Updated:** April 2026  
**Status:** Platform Standard

---

## Table of Contents

1. [Overview & Principles](#1-overview--principles)
2. [REST API Conventions](#2-rest-api-conventions)
3. [Standard Response Formats](#3-standard-response-formats)
4. [Error Handling & Status Codes](#4-error-handling--status-codes)
5. [Request Validation](#5-request-validation)
6. [API Versioning](#6-api-versioning)
7. [Pagination, Filtering & Sorting](#7-pagination-filtering--sorting)
8. [OpenAPI Documentation](#8-openapi-documentation)
9. [Security & Authentication](#9-security--authentication)
10. [Best Practices & Examples](#10-best-practices--examples)

---

## 1. Overview & Principles

### API Design Philosophy

Our APIs follow these core principles:

| Principle | Description |
|-----------|-------------|
| **RESTful** | Resource-oriented URLs, standard HTTP methods |
| **Consistent** | Predictable patterns across all services |
| **Self-Documenting** | OpenAPI/Swagger for interactive documentation |
| **Secure by Default** | OAuth2 JWT authentication, role-based access |
| **Observable** | Correlation IDs, structured logging, metrics |
| **Versioned** | Explicit versioning for backward compatibility |

### Quick Reference Card

```
POST   /api/v1/orders              → Create order (201 Created)
GET    /api/v1/orders/{id}         → Get order (200 OK)
GET    /api/v1/orders              → List orders (200 OK)
PUT    /api/v1/orders/{id}         → Update order (200 OK)
DELETE /api/v1/orders/{id}         → Delete order (204 No Content)

Standard Headers:
  Authorization: Bearer {jwt-token}
  X-Correlation-ID: {uuid}
  Content-Type: application/json
  Accept: application/json
```

---

## 2. REST API Conventions

### URL Structure

**Pattern:** `/api/v{version}/{resource}[/{id}][/{sub-resource}]`

**Examples:**
```
GET    /api/v1/orders                    # List all orders
GET    /api/v1/orders/ORD-12345          # Get specific order
GET    /api/v1/orders/ORD-12345/items    # Get order items
POST   /api/v1/orders                    # Create new order
PUT    /api/v1/orders/ORD-12345          # Update entire order
PATCH  /api/v1/orders/ORD-12345/status   # Partial update
DELETE /api/v1/orders/ORD-12345          # Delete order
```

### HTTP Methods & Semantics

| Method | Purpose | Request Body | Response Body | Idempotent |
|--------|---------|--------------|---------------|------------|
| **GET** | Retrieve resource(s) | ❌ No | ✅ Yes | ✅ Yes |
| **POST** | Create new resource | ✅ Yes | ✅ Yes (created resource) | ❌ No |
| **PUT** | Replace entire resource | ✅ Yes | ✅ Yes (updated resource) | ✅ Yes |
| **PATCH** | Partial update | ✅ Yes | ✅ Yes (updated resource) | ❌ No |
| **DELETE** | Remove resource | ❌ No | ❌ No (204) or ✅ Yes | ✅ Yes |

### Naming Conventions

**✅ DO:**
- Use **plural nouns** for collections: `/orders`, `/customers`
- Use **lowercase** and **kebab-case**: `/customer-profiles`
- Use **nouns**, not verbs: `/orders` not `/getOrders`
- Use **hierarchical** structure: `/orders/{id}/items`

**❌ DON'T:**
- Use verbs in URLs: `/createOrder`, `/deleteCustomer`
- Mix naming styles: `/customerProfiles`, `/customer_profiles`
- Use trailing slashes: `/orders/` (use `/orders`)
- Expose internal IDs in URLs (use business IDs when possible)

### Query Parameters

**Standard parameters:**

| Parameter | Purpose | Example |
|-----------|---------|---------|
| `page` | Page number (0-indexed) | `?page=0` |
| `size` | Page size (default 20, max 100) | `?size=50` |
| `sort` | Sort fields (comma-separated) | `?sort=createdAt,desc` |
| `filter` | Filter expression | `?filter=status:ACTIVE` |
| `fields` | Partial response fields | `?fields=id,name,status` |

**Examples:**
```
GET /api/v1/orders?page=0&size=20&sort=createdAt,desc
GET /api/v1/orders?filter=status:PENDING,amount>1000
GET /api/v1/customers?fields=id,name,email
```

---

## 3. Standard Response Formats

### Success Response Structure

All successful responses follow this structure:

```json
{
  "data": {
    // The actual resource or collection
  },
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:30:45.123Z"
  }
}
```

### Single Resource Response

**Example: GET /api/v1/orders/ORD-12345**

```json
{
  "data": {
    "id": "ORD-12345",
    "customerId": "CUST-67890",
    "status": "CONFIRMED",
    "totalAmount": 99.99,
    "currency": "USD",
    "items": [
      {
        "id": "ITEM-1",
        "productId": "PROD-100",
        "quantity": 2,
        "unitPrice": 49.99
      }
    ],
    "createdAt": "2026-04-20T10:00:00Z",
    "updatedAt": "2026-04-20T10:30:00Z"
  },
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:30:45.123Z"
  }
}
```

### Collection Response (Paginated)

**Example: GET /api/v1/orders?page=0&size=20**

```json
{
  "data": [
    {
      "id": "ORD-12345",
      "customerId": "CUST-67890",
      "status": "CONFIRMED",
      "totalAmount": 99.99
    },
    {
      "id": "ORD-12346",
      "customerId": "CUST-67891",
      "status": "PENDING",
      "totalAmount": 149.99
    }
  ],
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

### Create Response (201 Created)

**Example: POST /api/v1/orders**

**Request:**
```json
{
  "customerId": "CUST-67890",
  "items": [
    {
      "productId": "PROD-100",
      "quantity": 2
    }
  ]
}
```

**Response (201 Created):**
```http
HTTP/1.1 201 Created
Location: /api/v1/orders/ORD-12347
Content-Type: application/json

{
  "data": {
    "id": "ORD-12347",
    "customerId": "CUST-67890",
    "status": "PENDING",
    "totalAmount": 99.98,
    "createdAt": "2026-04-20T10:35:00Z"
  },
  "metadata": {
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2026-04-20T10:35:00.456Z"
  }
}
```

### Empty Response (204 No Content)

**Example: DELETE /api/v1/orders/ORD-12345**

```http
HTTP/1.1 204 No Content
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
```

---

## 4. Error Handling & Status Codes

### Standard Error Response Format

**All errors follow this structure:**

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "email",
        "message": "must be a well-formed email address",
        "rejectedValue": "invalid-email"
      }
    ],
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/customers"
  }
}
```

### HTTP Status Codes

#### Success Codes (2xx)

| Code | Name | Usage |
|------|------|-------|
| **200** | OK | Successful GET, PUT, PATCH |
| **201** | Created | Successful POST (resource created) |
| **202** | Accepted | Async operation initiated |
| **204** | No Content | Successful DELETE or operation with no response body |

#### Client Error Codes (4xx)

| Code | Name | Error Code | Usage |
|------|------|------------|-------|
| **400** | Bad Request | `VALIDATION_ERROR` | Malformed request, validation failure |
| **401** | Unauthorized | `AUTHENTICATION_REQUIRED` | Missing or invalid authentication |
| **403** | Forbidden | `INSUFFICIENT_PERMISSIONS` | Authenticated but not authorized |
| **404** | Not Found | `RESOURCE_NOT_FOUND` | Resource doesn't exist |
| **409** | Conflict | `RESOURCE_CONFLICT` | Duplicate resource, state conflict |
| **422** | Unprocessable Entity | `BUSINESS_RULE_VIOLATION` | Valid syntax but business rule failed |
| **429** | Too Many Requests | `RATE_LIMIT_EXCEEDED` | Rate limit exceeded |

#### Server Error Codes (5xx)

| Code | Name | Error Code | Usage |
|------|------|------------|-------|
| **500** | Internal Server Error | `INTERNAL_ERROR` | Unexpected server error |
| **502** | Bad Gateway | `UPSTREAM_ERROR` | Upstream service error |
| **503** | Service Unavailable | `SERVICE_UNAVAILABLE` | Service temporarily unavailable |
| **504** | Gateway Timeout | `UPSTREAM_TIMEOUT` | Upstream service timeout |

### Error Response Examples

#### Validation Error (400)

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "email",
        "message": "must be a well-formed email address",
        "rejectedValue": "bad-email"
      },
      {
        "field": "age",
        "message": "must be greater than or equal to 18",
        "rejectedValue": 15
      }
    ],
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/customers"
  }
}
```

#### Authentication Error (401)

```json
{
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "Authentication token is missing or invalid",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/orders"
  }
}
```

#### Authorization Error (403)

```json
{
  "error": {
    "code": "INSUFFICIENT_PERMISSIONS",
    "message": "User does not have required role: ADMIN",
    "details": [
      {
        "requiredRoles": ["ADMIN"],
        "userRoles": ["USER"]
      }
    ],
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/admin/users"
  }
}
```

#### Resource Not Found (404)

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Order not found: ORD-99999",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/orders/ORD-99999"
  }
}
```

#### Business Rule Violation (422)

```json
{
  "error": {
    "code": "BUSINESS_RULE_VIOLATION",
    "message": "Cannot cancel order in SHIPPED status",
    "details": [
      {
        "rule": "ORDER_CANCELLATION_RULE",
        "currentStatus": "SHIPPED",
        "allowedStatuses": ["PENDING", "CONFIRMED"]
      }
    ],
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/orders/ORD-12345/cancel"
  }
}
```

#### Internal Server Error (500)

```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "An unexpected error occurred while processing your request",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000",
    "path": "/api/v1/orders"
  }
}
```

**Note:** Stack traces are **NEVER** included in production error responses. Use correlation ID to find detailed logs.

---

## 5. Request Validation

### Bean Validation Annotations

Use standard **Jakarta Bean Validation** annotations:

```java
public class CreateCustomerRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Age is required")
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid E.164 format")
    private String phoneNumber;
    
    @NotEmpty(message = "At least one address is required")
    @Valid  // Cascade validation to nested objects
    private List<Address> addresses;
    
    // Getters/Setters
}
```

### Common Validation Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@NotNull` | Field must not be null | `@NotNull private String id;` |
| `@NotBlank` | String must not be null/empty/whitespace | `@NotBlank private String name;` |
| `@NotEmpty` | Collection must not be null/empty | `@NotEmpty private List<String> tags;` |
| `@Size` | String/Collection size constraints | `@Size(min=2, max=50)` |
| `@Min` / `@Max` | Numeric min/max values | `@Min(0) @Max(100)` |
| `@Email` | Valid email format | `@Email private String email;` |
| `@Pattern` | Regex pattern matching | `@Pattern(regexp="^[A-Z]{3}$")` |
| `@Past` / `@Future` | Date in past/future | `@Past private LocalDate birthDate;` |
| `@Valid` | Cascade validation to nested objects | `@Valid private Address address;` |

### Controller Validation

```java
@PostMapping("/api/v1/customers")
public ResponseEntity<ApiResponse<Customer>> createCustomer(
        @Valid @RequestBody CreateCustomerRequest request) {
    // Validation happens automatically before method execution
    Customer customer = customerService.create(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION, "/api/v1/customers/" + customer.getId())
        .body(ApiResponse.success(customer));
}
```

### Custom Validation

**Custom annotation:**
```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderStatusValidator.class)
public @interface ValidOrderStatus {
    String message() default "Invalid order status";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class OrderStatusValidator implements ConstraintValidator<ValidOrderStatus, String> {
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || VALID_STATUSES.contains(value);
    }
}
```

**Usage:**
```java
public class UpdateOrderRequest {
    @ValidOrderStatus
    private String status;
}
```

---

## 6. API Versioning

### Versioning Strategy: URI Versioning

**Pattern:** `/api/v{major-version}/{resource}`

**Rationale:**
- **Simple:** Easy to understand and implement
- **Discoverable:** Version visible in URL
- **Cacheable:** Different URLs = different cache entries
- **Browser-friendly:** Can test in browser directly

### Version Numbering

- **Major version only:** `v1`, `v2`, `v3`
- **Increment on breaking changes:**
  - Removing fields
  - Renaming fields
  - Changing field types
  - Changing URL structure
  - Changing authentication mechanism

### Backward Compatibility

**✅ Non-Breaking Changes (same version):**
- Adding new optional fields
- Adding new endpoints
- Adding new optional query parameters
- Deprecating fields (keep them, mark as deprecated)

**❌ Breaking Changes (new version):**
- Removing fields
- Renaming fields
- Changing field types (string → number)
- Making optional fields required
- Changing error response format

### Version Lifecycle

| Stage | Description | Support Level |
|-------|-------------|---------------|
| **Current** | Latest version | ✅ Full support, new features |
| **Deprecated** | Previous version | ⚠️ Maintenance only, no new features |
| **End-of-Life** | Old version | ❌ No support, may be removed |

**Example Timeline:**
```
2026-01-01: v2 released (v1 deprecated, 12-month notice)
2026-06-01: v3 released (v2 current, v1 still supported)
2027-01-01: v1 end-of-life (removed)
```

### Deprecation Headers

**Deprecated API response includes:**
```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jan 2027 00:00:00 GMT
Link: </api/v2/orders>; rel="successor-version"
```

### Implementation Example

```java
// v1 Controller
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {
    // Old implementation
}

// v2 Controller
@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {
    // New implementation with breaking changes
}
```

---

## 7. Pagination, Filtering & Sorting

### Pagination (Page-Based)

**Request:**
```
GET /api/v1/orders?page=0&size=20
```

**Response:**
```json
{
  "data": [ /* 20 orders */ ],
  "metadata": {
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

**Defaults:**
- `page`: 0 (first page)
- `size`: 20 (configurable max: 100)

### Sorting

**Single field:**
```
GET /api/v1/orders?sort=createdAt,desc
```

**Multiple fields:**
```
GET /api/v1/orders?sort=status,asc&sort=createdAt,desc
```

**Format:** `{field},{direction}`
- Direction: `asc` (default) or `desc`

### Filtering

**Simple filter (equality):**
```
GET /api/v1/orders?filter=status:PENDING
```

**Multiple filters (AND):**
```
GET /api/v1/orders?filter=status:PENDING,customerId:CUST-123
```

**Operators:**

| Operator | Symbol | Example |
|----------|--------|---------|
| Equal | `:` | `status:PENDING` |
| Greater than | `>` | `amount>100` |
| Less than | `<` | `amount<500` |
| Greater or equal | `>=` | `amount>=100` |
| Less or equal | `<=` | `amount<=500` |
| Not equal | `!` | `status!CANCELLED` |
| Like | `~` | `name~john` |

**Complex example:**
```
GET /api/v1/orders?filter=status:PENDING,amount>=100,amount<=500,customerId:CUST-123
```

### Partial Response (Field Selection)

**Request:**
```
GET /api/v1/orders/ORD-12345?fields=id,status,totalAmount
```

**Response (only requested fields):**
```json
{
  "data": {
    "id": "ORD-12345",
    "status": "CONFIRMED",
    "totalAmount": 99.99
  }
}
```

### Full Example

```
GET /api/v1/orders
  ?page=1
  &size=50
  &sort=createdAt,desc
  &filter=status:PENDING,amount>=100
  &fields=id,customerId,status,totalAmount,createdAt
```

---

## 8. OpenAPI Documentation

### Swagger/OpenAPI Integration

**Configuration class:**
```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Order Management API")
                .version("1.0.0")
                .description("RESTful API for order management")
                .contact(new Contact()
                    .name("Platform Team")
                    .email("platform-team@company.com"))
                .license(new License()
                    .name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("https://api-staging.company.com").description("Staging"),
                new Server().url("https://api.company.com").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

### Annotating Controllers

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {
    
    @Operation(
        summary = "Create a new order",
        description = "Creates a new order for the authenticated customer"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @Parameter(description = "Order details", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        // Implementation
    }
    
    @Operation(summary = "Get order by ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrder(
            @Parameter(description = "Order ID", example = "ORD-12345")
            @PathVariable String orderId) {
        // Implementation
    }
}
```

### Annotating Models

```java
@Schema(description = "Order creation request")
public class CreateOrderRequest {
    
    @Schema(description = "Customer ID", example = "CUST-67890", required = true)
    @NotBlank
    private String customerId;
    
    @Schema(description = "Order items", required = true)
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
    
    @Schema(description = "Shipping address")
    @Valid
    private Address shippingAddress;
}
```

### Accessing Documentation

**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`  
**OpenAPI YAML:** `http://localhost:8080/v3/api-docs.yaml`

---

## 9. Security & Authentication

### Authentication (OAuth2 JWT)

**Request header:**
```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token claims:**
```json
{
  "sub": "john.doe@company.com",
  "name": "John Doe",
  "roles": ["USER", "CUSTOMER"],
  "aud": "order-service",
  "iss": "https://auth.company.com",
  "exp": 1713618645
}
```

### Authorization (Role-Based)

**Controller-level:**
```java
@RestController
@RequestMapping("/api/v1/admin")
@RequireRoles("ADMIN")
public class AdminController {
    // All endpoints require ADMIN role
}
```

**Method-level:**
```java
@GetMapping("/api/v1/orders/{id}")
@RequireRoles({"USER", "ADMIN"})  // Either role allowed
public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable String id) {
    // Implementation
}

@DeleteMapping("/api/v1/orders/{id}")
@RequireRoles("ADMIN")  // Only ADMIN allowed
public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
    // Implementation
}
```

### Security Headers

**Required request headers:**
- `Authorization: Bearer {token}` - JWT authentication token
- `X-Correlation-ID: {uuid}` - Request correlation ID (optional, auto-generated if missing)

**Response headers:**
- `X-Correlation-ID: {uuid}` - Echo correlation ID for tracing
- `X-Content-Type-Options: nosniff` - Prevent MIME type sniffing
- `X-Frame-Options: DENY` - Prevent clickjacking
- `Strict-Transport-Security: max-age=31536000` - Force HTTPS

### Rate Limiting

**Default limits:**
- Anonymous: 100 requests/hour
- Authenticated: 1000 requests/hour
- Admin: 5000 requests/hour

**Response when rate limit exceeded (429):**
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1713619200
Retry-After: 3600

{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "API rate limit exceeded. Try again in 3600 seconds.",
    "timestamp": "2026-04-20T10:30:45.123Z",
    "correlationId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## 10. Best Practices & Examples

### Complete CRUD Example

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management API")
@RequireRoles({"USER", "ADMIN"})
public class OrderController {
    
    private final OrderService orderService;
    
    // CREATE
    @PostMapping
    @Operation(summary = "Create a new order")
    @Timed(value = "api.orders.create", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        Order order = orderService.create(request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(HttpHeaders.LOCATION, "/api/v1/orders/" + order.getId())
            .body(ApiResponse.success(order));
    }
    
    // READ (single)
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    @Timed(value = "api.orders.get", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<Order>> getOrder(
            @PathVariable String orderId) {
        
        Order order = orderService.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    // READ (collection)
    @GetMapping
    @Operation(summary = "List orders")
    @Timed(value = "api.orders.list", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<List<Order>>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sort) {
        
        Page<Order> orderPage = orderService.findAll(page, size, filter, sort);
        
        return ResponseEntity.ok(ApiResponse.success(
            orderPage.getContent(),
            PaginationMetadata.from(orderPage)
        ));
    }
    
    // UPDATE (full)
    @PutMapping("/{orderId}")
    @Operation(summary = "Update order")
    @Timed(value = "api.orders.update", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<Order>> updateOrder(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderRequest request) {
        
        Order order = orderService.update(orderId, request);
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    // UPDATE (partial)
    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    @Timed(value = "api.orders.updateStatus", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateStatusRequest request) {
        
        Order order = orderService.updateStatus(orderId, request.getStatus());
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    // DELETE
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order")
    @RequireRoles("ADMIN")  // More restrictive than controller-level
    @Timed(value = "api.orders.delete", percentiles = {0.95, 0.99})
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        
        orderService.cancel(orderId);
        
        return ResponseEntity.noContent().build();
    }
}
```

### DTOs (Data Transfer Objects)

**Request DTO:**
```java
@Schema(description = "Order creation request")
public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    @Schema(description = "Customer identifier", example = "CUST-67890")
    private String customerId;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    @Schema(description = "Order items")
    private List<OrderItemRequest> items;
    
    @Schema(description = "Special instructions")
    @Size(max = 500)
    private String notes;
    
    // Getters/Setters
}

@Schema(description = "Order item")
public class OrderItemRequest {
    
    @NotBlank
    @Schema(description = "Product ID", example = "PROD-100")
    private String productId;
    
    @NotNull
    @Min(1)
    @Schema(description = "Quantity", example = "2")
    private Integer quantity;
    
    // Getters/Setters
}
```

**Response DTO:**
```java
@Schema(description = "Order details")
public class OrderResponse {
    
    @Schema(description = "Order ID", example = "ORD-12345")
    private String id;
    
    @Schema(description = "Customer ID", example = "CUST-67890")
    private String customerId;
    
    @Schema(description = "Order status", example = "CONFIRMED")
    private OrderStatus status;
    
    @Schema(description = "Total amount", example = "99.99")
    private BigDecimal totalAmount;
    
    @Schema(description = "Currency code", example = "USD")
    private String currency;
    
    @Schema(description = "Order items")
    private List<OrderItemResponse> items;
    
    @Schema(description = "Creation timestamp")
    private Instant createdAt;
    
    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
    
    // Getters/Setters
}
```

### Exception Handling Best Practices

**Custom exceptions:**
```java
// Domain exception
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;
    
    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }
}

// Business rule exception
public class InvalidOrderStateException extends RuntimeException {
    private final String orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;
    
    public InvalidOrderStateException(String orderId, OrderStatus current, OrderStatus target) {
        super(String.format("Cannot transition order %s from %s to %s", orderId, current, target));
        this.orderId = orderId;
        this.currentStatus = current;
        this.targetStatus = target;
    }
}
```

### Logging Best Practices

**✅ DO:**
```java
@PostMapping
public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    log.info("Creating order for customer: {}", request.getCustomerId());
    
    try {
        Order order = orderService.create(request);
        log.info("Order created successfully: orderId={}, amount={}", order.getId(), order.getTotalAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
        
    } catch (InsufficientInventoryException e) {
        log.warn("Order creation failed due to insufficient inventory: customerId={}, productId={}", 
            request.getCustomerId(), e.getProductId());
        throw e;
    }
}
```

**❌ DON'T:**
```java
// Don't log sensitive data
log.info("Creating order with payment details: {}", request.getPaymentCard());

// Don't log entire objects (may contain sensitive fields)
log.info("Request: {}", request);

// Don't use string concatenation
log.info("Order created: " + orderId + " for customer: " + customerId);
```

### Testing Checklist

**For each endpoint, test:**
- ✅ Happy path (200/201/204)
- ✅ Validation errors (400)
- ✅ Authentication required (401)
- ✅ Authorization denied (403)
- ✅ Resource not found (404)
- ✅ Business rule violations (422)
- ✅ Correlation ID in response
- ✅ OpenAPI documentation generated
- ✅ Metrics recorded (@Timed)
- ✅ Structured logs with MDC fields

---

## Quick Decision Tree

**Choosing HTTP Method:**
```
Creating new resource?
  → POST /api/v1/orders

Replacing entire resource?
  → PUT /api/v1/orders/{id}

Updating specific field(s)?
  → PATCH /api/v1/orders/{id}/status

Retrieving resource(s)?
  → GET /api/v1/orders[/{id}]

Deleting resource?
  → DELETE /api/v1/orders/{id}
```

**Choosing Status Code:**
```
Request successful?
  → Created something? → 201
  → Updated/retrieved something? → 200
  → Deleted something? → 204

Client error?
  → Invalid input? → 400
  → Not authenticated? → 401
  → Not authorized? → 403
  → Not found? → 404
  → State conflict? → 409
  → Business rule violated? → 422

Server error?
  → Unexpected error? → 500
  → External service failed? → 502/504
```

---

## Additional Resources

- **OpenAPI Specification:** https://spec.openapis.org/oas/latest.html
- **REST API Best Practices:** https://restfulapi.net/
- **HTTP Status Codes:** https://httpstatuses.com/
- **Jakarta Bean Validation:** https://beanvalidation.org/
- **Spring Boot REST Docs:** https://spring.io/guides/gs/rest-service/

**Internal Resources:**
- Platform Team: `#platform-api-standards` (Slack)
- API Review Process: `wiki.company.com/api-review`
- Reference Implementation: `service-template` (this repo)

---

**Last Updated:** April 20, 2026  
**Version:** 1.0.0  
**Maintained By:** Platform Engineering Team
