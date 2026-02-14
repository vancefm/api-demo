# Computer Systems Management API

A comprehensive Spring Boot REST API demonstration for managing computer systems with enterprise-level features.

- [Computer Systems Management API](#computer-systems-management-api)
  - [Features](#features)
  - [Technologies Used](#technologies-used)
  - [Architecture \& Design](#architecture--design)
    - [API Versioning](#api-versioning)
      - [Current Implementation](#current-implementation)
    - [Error Handling](#error-handling)
      - [Error Codes](#error-codes)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Build](#build)
    - [Run](#run)
  - [API Documentation](#api-documentation)
    - [ComputerSystem Entity Fields](#computersystem-entity-fields)
    - [API Endpoints](#api-endpoints)
    - [Create Computer System](#create-computer-system)
    - [Get All Computer Systems (with pagination)](#get-all-computer-systems-with-pagination)
    - [Get Computer System by ID](#get-computer-system-by-id)
    - [Get Computer System by Hostname](#get-computer-system-by-hostname)
    - [Filter Computer Systems](#filter-computer-systems)
    - [Update Computer System](#update-computer-system)
    - [Delete Computer System](#delete-computer-system)
  - [Query Features](#query-features)
    - [Pagination and Sorting](#pagination-and-sorting)
    - [Filtering](#filtering)
  - [Data Validation](#data-validation)
    - [Validation Rules](#validation-rules)
  - [Development \& Testing](#development--testing)
    - [Running Tests](#running-tests)
    - [Test Architecture](#test-architecture)
      - [1. Repository Tests (`@DataJpaTest`)](#1-repository-tests-datajpatest)
      - [2. Service Tests (Unit Tests)](#2-service-tests-unit-tests)
      - [3. Controller Tests](#3-controller-tests)
      - [4. Integration Tests (`@SpringBootTest` + `@Transactional`)](#4-integration-tests-springboottest--transactional)
    - [Configuration](#configuration)
    - [Logging](#logging)
    - [Access Swagger UI](#access-swagger-ui)
    - [Access H2 Console](#access-h2-console)
  - [Advanced Topics](#advanced-topics)
    - [Rate Limiting](#rate-limiting)
      - [How It Works](#how-it-works)
      - [Configuration](#configuration-1)
      - [Implementation Details](#implementation-details)
      - [Rate Limit Exceeded Response](#rate-limit-exceeded-response)
      - [Testing Rate Limits](#testing-rate-limits)
      - [Monitoring Rate Limiting](#monitoring-rate-limiting)
      - [Extending Rate Limiting](#extending-rate-limiting)
        - [Option 1: Adjust Global Limits](#option-1-adjust-global-limits)
        - [Option 2: Add Per-Endpoint Limits](#option-2-add-per-endpoint-limits)
        - [Option 3: Client-Based Rate Limiting](#option-3-client-based-rate-limiting)
      - [Best Practices](#best-practices)
    - [Request/Response Compression](#requestresponse-compression)
      - [Configuration](#configuration-2)
      - [How It Works](#how-it-works-1)
      - [Performance Benefits](#performance-benefits)
      - [Testing Compression](#testing-compression)
      - [Best Practices](#best-practices-1)
    - [Circuit Breaker Pattern](#circuit-breaker-pattern)
      - [Use Cases](#use-cases)
      - [How It Works](#how-it-works-2)
      - [Configuration](#configuration-3)
      - [Implementation Examples](#implementation-examples)
      - [Monitoring](#monitoring)
      - [Best Practices](#best-practices-2)
    - [Batch Operations](#batch-operations)
      - [All-or-Nothing Semantics](#all-or-nothing-semantics)
      - [Configuration](#configuration-4)
      - [API Endpoints](#api-endpoints-1)
      - [Batch Create](#batch-create)
      - [Batch Update](#batch-update)
      - [Batch Delete](#batch-delete)
      - [Validation](#validation)
      - [Error Handling](#error-handling-1)
      - [Best Practices](#best-practices-3)
    - [API Versioning Strategy](#api-versioning-strategy)
      - [Current Implementation](#current-implementation-1)
      - [When to Create a New Version](#when-to-create-a-new-version)
      - [Creating a New Version](#creating-a-new-version)
      - [Best Practices](#best-practices-4)
    - [Spring Boot Actuator](#spring-boot-actuator)
      - [What Actuator Provides](#what-actuator-provides)
      - [Configuration](#configuration-5)
      - [Health Checks](#health-checks)
      - [Monitoring Rate Limiter Metrics](#monitoring-rate-limiter-metrics)
      - [Integration with Monitoring Systems](#integration-with-monitoring-systems)
      - [Example Kubernetes Configuration](#example-kubernetes-configuration)
      - [Best Practices](#best-practices-5)
  - [Recent Updates (November 2025)](#recent-updates-november-2025)
    - [RFC 9457 ProblemDetail Migration](#rfc-9457-problemdetail-migration)
  - [Observability \& Metrics](#observability--metrics)
    - [Accessing Metrics](#accessing-metrics)
      - [Get All Metrics](#get-all-metrics)
      - [Get Specific Metric Value](#get-specific-metric-value)
      - [Get Metrics in Prometheus Format](#get-metrics-in-prometheus-format)
    - [Available Custom Metrics](#available-custom-metrics)
      - [`app.computersystems.total` (GAUGE)](#appcomputersystemstotal-gauge)
    - [Adding New Metrics](#adding-new-metrics)
      - [1. Create a Metric Component](#1-create-a-metric-component)
      - [2. Naming Convention](#2-naming-convention)
      - [3. Automatic Registration](#3-automatic-registration)
    - [Monitoring and Observability](#monitoring-and-observability)
      - [Spring Boot Health Endpoint](#spring-boot-health-endpoint)
      - [Integrating with Monitoring Systems](#integrating-with-monitoring-systems)
    - [Metrics Configuration](#metrics-configuration)
  - [Authentication \& Authorization](#authentication--authorization)
    - [Overview](#overview)
    - [Key Features](#key-features)
    - [Architecture \& Flow](#architecture--flow)
    - [Database Schema](#database-schema)
      - [Roles Table](#roles-table)
      - [Permissions Table](#permissions-table)
      - [Role\_Permissions Table (Junction)](#role_permissions-table-junction)
      - [Users Table](#users-table)
    - [Default Roles](#default-roles)
      - [MY\_APP\_SUPERADMIN](#my_app_superadmin)
      - [MY\_APP\_ADMIN](#my_app_admin)
      - [MY\_APP\_USER](#my_app_user)
    - [Field Permission Configuration](#field-permission-configuration)
    - [Admin APIs](#admin-apis)
      - [Role Management](#role-management)
      - [Permission Management](#permission-management)
      - [Role-Permission Assignment](#role-permission-assignment)
      - [User Management](#user-management)
      - [Cache Management](#cache-management)
    - [Step-by-Step: Adding a New Object Type](#step-by-step-adding-a-new-object-type)
      - [Step 1: Define the Entity](#step-1-define-the-entity)
      - [Step 2: Update FieldPermissionsConfig](#step-2-update-fieldpermissionsconfig)
      - [Step 3: Create Permissions via Admin API](#step-3-create-permissions-via-admin-api)
      - [Step 4: Assign Permissions to Roles](#step-4-assign-permissions-to-roles)
      - [Step 5: Reload Cache](#step-5-reload-cache)
    - [Step-by-Step: Adding a New Field to Existing Object](#step-by-step-adding-a-new-field-to-existing-object)
      - [Step 1: Update Entity](#step-1-update-entity)
      - [Step 2: Update DTO](#step-2-update-dto)
      - [Step 3: Decide Field Permissions](#step-3-decide-field-permissions)
      - [Step 4: Update Permissions via Admin API](#step-4-update-permissions-via-admin-api)
      - [Step 5: Reload Cache](#step-5-reload-cache-1)
    - [Security Best Practices](#security-best-practices)
    - [Testing Scenarios](#testing-scenarios)
      - [Test 1: User Can Only Access Own Resources](#test-1-user-can-only-access-own-resources)
      - [Test 2: Admin Can Access Department Resources](#test-2-admin-can-access-department-resources)
      - [Test 3: Field-Level Restrictions](#test-3-field-level-restrictions)
      - [Test 4: Super Admin Has Full Access](#test-4-super-admin-has-full-access)
    - [Troubleshooting Guide](#troubleshooting-guide)
      - [Problem: Permission changes not taking effect](#problem-permission-changes-not-taking-effect)
      - [Problem: User cannot access their own resources](#problem-user-cannot-access-their-own-resources)
      - [Problem: Field appears in response but shouldn't](#problem-field-appears-in-response-but-shouldnt)
      - [Problem: Cannot create/update role](#problem-cannot-createupdate-role)
      - [Problem: Slow permission checks](#problem-slow-permission-checks)
    - [Performance Considerations](#performance-considerations)
    - [Future Enhancements](#future-enhancements)
  - [Ideas for future Enhancements](#ideas-for-future-enhancements)
  - [License](#license)
  - [Support](#support)


## Features

- **RESTful API**: Full CRUD operations with proper HTTP methods and status codes
- **API Versioning**: Version-based URI routing (`/api/v1/...`)
- **Service Layer**: Clean separation of concerns with business logic in service layer
- **Exception Handling**: Global exception handling with custom error responses
- **Data Transfer Objects (DTOs)**: Decoupled request/response models
- **Input Validation**: Comprehensive validation with meaningful error messages
- **API Documentation**: Swagger/OpenAPI with interactive UI
- **Pagination**: Support for page, size, and sorting
- **Filtering**: Advanced filtering capabilities
- **Sorting**: Customizable sorting by any field
- **Rate Limiting**: Global rate limiting with Resilience4j to protect against abuse
- **Response Compression**: GZIP compression for improved performance and bandwidth savings
- **Logging**: Comprehensive logging at service and controller levels
- **Constructor Injection**: Dependency injection via constructors
- **Database**: H2 in-memory database with JPA/Hibernate ORM
- **Testing**: Unit and integration tests with JUnit 5 and Mockito
- **Constructor Injection**: Dependency injection via constructors
- **Database**: H2 in-memory database with JPA/Hibernate ORM
- **Testing**: Unit and integration tests with JUnit 5 and Mockito

## Technologies Used

- **Spring Boot 3.5.8**: Latest stable LTS framework
- **Java 21**: Latest LTS programming language
- **Spring Data JPA**: ORM and repository pattern
- **Hibernate**: JPA implementation
- **H2 Database**: In-memory relational database
- **SpringDoc OpenAPI 2.3.0**: Swagger/OpenAPI documentation
- **Lombok**: Reduce boilerplate code
- **JUnit 5**: Testing framework
- **Mockito 5.11.0**: Mocking framework with Java 21+ support
- **Maven 3.9.11**: Build tool and dependency management

## Architecture & Design

### API Versioning

This API uses **URI path versioning** for managing different API versions. All endpoints are prefixed with a version identifier (e.g., `/api/v1/`).

#### Current Implementation

All endpoints use the `/api/v1/` prefix:
- `POST /api/v1/computer-systems`
- `GET /api/v1/computer-systems/{id}`
- `PUT /api/v1/computer-systems/{id}`
- `DELETE /api/v1/computer-systems/{id}`

### Error Handling

The API returns structured error responses following **RFC 9457** (Problem Details for HTTP APIs) using Spring Boot's `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Computer system with id 99 not found",
  "instance": "/api/v1/computer-systems/99",
  "timestamp": "2025-11-30T15:30:00Z"
}
```

**RFC 9457 Benefits:**
- **Standardized Format**: Interoperable across all clients and tools
- **Spring Native**: Uses `ProblemDetail` built into Spring Framework 6.2+
- **Extensible**: Add custom properties for domain-specific error information
- **Semantic**: Standard field names (`title`, `detail`, `status`, `instance`) recognized by API clients

**Field Mapping:**
| RFC 9457 Field | Purpose |
|---|---|
| `type` | Problem type URI (usually "about:blank" for general errors) |
| `title` | Short, human-readable error title |
| `status` | HTTP status code |
| `detail` | Extended error description (may include field-level validation details) |
| `instance` | URI identifying the specific error occurrence (request path) |
| `timestamp` | ISO 8601 timestamp of error occurrence |

**Validation Errors with Batch Operations:**
When validation fails, the `detail` field includes item-level error information:

```json
{
  "type": "about:blank",
  "title": "Request Validation Failed",
  "status": 400,
  "detail": "items[0].ipAddress: Invalid IP address format; items[1].macAddress: Invalid MAC address format",
  "instance": "/api/v1/computer-systems/batch/create",
  "timestamp": "2025-11-30T15:30:00Z"
}
```

#### Error Codes
- **400**: Validation errors (field-level details in `detail` field)
- **404**: Resource not found
- **409**: Duplicate resource
- **503**: Service temporarily unavailable (circuit breaker open)
- **500**: Internal server error

## Getting Started

### Prerequisites
- Java 21 or higher (LTS version)
- Maven 3.8.0 or higher

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

### ComputerSystem Entity Fields

- **id**: Unique identifier (auto-generated)
- **hostname**: Computer hostname (unique, required)
- **manufacturer**: Computer manufacturer (required)
- **model**: Computer model (required)
- **user**: Computer user (required)
- **department**: Department (required)
- **macAddress**: MAC address (unique, required, validated)
- **ipAddress**: IP address (unique, required, validated)
- **networkName**: Network name (required)

### API Endpoints

### Create Computer System
```
POST /api/v1/computer-systems
Content-Type: application/json

{
  "hostname": "SERVER-001",
  "manufacturer": "Dell",
  "model": "PowerEdge R750",
  "user": "john.doe",
  "department": "IT",
  "macAddress": "00:1A:2B:3C:4D:5E",
  "ipAddress": "192.168.1.100",
  "networkName": "PROD-NETWORK"
}
```

### Get All Computer Systems (with pagination)
```
GET /api/v1/computer-systems?page=0&size=20&sort=hostname,asc
```

### Get Computer System by ID
```
GET /api/v1/computer-systems/{id}
```

### Get Computer System by Hostname
```
GET /api/v1/computer-systems/hostname/{hostname}
```

### Filter Computer Systems
```
GET /api/v1/computer-systems/filter?hostname=SERVER&department=IT&user=john&page=0&size=20&sort=id,desc
```

### Update Computer System
```
PUT /api/v1/computer-systems/{id}
Content-Type: application/json

{
  "hostname": "SERVER-002",
  "manufacturer": "HP",
  "model": "ProLiant DL380",
  "user": "jane.doe",
  "department": "HR",
  "macAddress": "00:1A:2B:3C:4D:5F",
  "ipAddress": "192.168.1.101",
  "networkName": "PROD-NETWORK"
}
```

### Delete Computer System
```
DELETE /api/v1/computer-systems/{id}
```

## Query Features

### Pagination and Sorting

All list endpoints support pagination:
- `page`: Zero-indexed page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Field and direction (format: `field,asc` or `field,desc`)

Example:
```
GET /api/v1/computer-systems?page=1&size=10&sort=hostname,asc
```

### Filtering

Filter endpoints support the following query parameters:
- `hostname`: Partial match (case-insensitive)
- `department`: Exact match
- `user`: Partial match (case-insensitive)

Example:
```
GET /api/v1/computer-systems/filter?hostname=SERVER&department=IT&user=john
```

## Data Validation

### Validation Rules

- **hostname**: Required, must be unique
- **manufacturer**: Required
- **model**: Required
- **user**: Required
- **department**: Required
- **macAddress**: Required, must match pattern `XX:XX:XX:XX:XX:XX`
- **ipAddress**: Required, must be valid IPv4 format
- **networkName**: Required

## Development & Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ComputerSystemIntegrationTest

# Run with coverage
mvn test jacoco:report
```

Test Summary:
- **Total Tests**: 56 (all passing ✅)
- **Repository Tests**: 7 (Data access layer - `@DataJpaTest`)
- **Service Tests**: 10 (Business logic - mocked dependencies)
- **Controller Tests**: 7 (REST endpoints - mocked services)
- **Integration Tests**: 6 (Full application - real database with `@Transactional`)
- **Batch Controller Tests**: 10 (Batch operations)
- **LDAP Authentication Tests**: 6 (Embedded LDAP authentication and role mapping)
- **LDAP Integration Tests**: 8 (End-to-end login flow and access control with embedded LDAP)
- **Security Token Tests**: 2 (Token creation and controller tests)

### Test Architecture

#### 1. Repository Tests (`@DataJpaTest`)
- **Purpose**: Test data access layer directly
- **Database**: H2 in-memory (auto-rollback)
- **Example**: CRUD operations, query methods, database constraints

#### 2. Service Tests (Unit Tests)
- **Purpose**: Test business logic in isolation
- **Dependencies**: Mocked (repositories, external services)
- **Database**: None (mocked)
- **Example**: Service methods, validation logic

#### 3. Controller Tests
- **Purpose**: Test REST endpoints with mocked services
- **Dependencies**: Mocked services
- **Database**: None
- **Example**: HTTP status codes, response structure

#### 4. Integration Tests (`@SpringBootTest` + `@Transactional`)
- **Purpose**: Test entire application end-to-end
- **Database**: Real H2 in-memory database
- **Key Feature**: `@Transactional` annotation ensures:
  - Each test runs in its own transaction
  - All database changes automatically **rollback** after test completes
  - Ensures **isolation** - tests don't interfere with each other
  - Prevents **data pollution** if tests fail
  - Faster than manual cleanup with DELETE statements

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // ← Enables automatic rollback
class ComputerSystemIntegrationTest {
    @Test
    void testCreateAndRetrieve() throws Exception {
        // INSERT happens within transaction
        mockMvc.perform(post("/api/v1/computer-systems")...)
                .andExpect(status().isCreated());
        
        // SELECT happens within transaction
        mockMvc.perform(get("/api/v1/computer-systems/1"))
                .andExpect(status().isOk());
        
    } // ← After test: automatic ROLLBACK
}
```

#### 5. LDAP Authentication Tests (`@SpringBootTest` + `@Import`)
- **Purpose**: Verify embedded LDAP authentication and LDAP group → application role mapping
- **LDAP Server**: Embedded UnboundID in-memory LDAP server started by `EmbeddedLdapTestConfig`
- **Users**: Seeded from `test-ldap-users.ldif` (`user1`, `user2`, `admin1`, `superadmin1`)
- **Key Assertions**: Correct roles assigned per group membership, dual-group membership (admin1), bad credentials rejected

#### 6. LDAP Integration Tests (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Import`)
- **Purpose**: End-to-end HTTP tests of login flow and endpoint access control using LDAP credentials
- **Security Filters**: Enabled (no `addFilters=false`)
- **Key Assertions**: Login returns JWT, invalid credentials return 401, regular users cannot access admin endpoints, super-admins can access admin endpoints

### Configuration

Application configuration is in `src/main/resources/application.yml`:

- **Server Port**: 8080
- **Database**: H2 in-memory
- **DDL**: create-drop (creates tables on startup, drops on shutdown)
- **Logging Level**: DEBUG for `com.demo`, INFO for others
- **Log File**: `logs/application.log`

### Logging

Logging is configured for different levels:
- **DEBUG**: `com.demo` package (application code)
- **INFO**: Spring framework and root logger
- Logs are written to both console and `logs/application.log`

Sample log format:
```
2025-11-27 10:30:45 - REST call: POST /api/v1/computer-systems
2025-11-27 10:30:46 - Creating new computer system with hostname: SERVER-001
2025-11-27 10:30:46 - Computer system created successfully with id: 1
```

### Access Swagger UI
Navigate to `http://localhost:8080/swagger-ui.html` to view and test the API interactively.

### Access H2 Console
Navigate to `http://localhost:8080/h2-console` to access the H2 database console.
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

## Advanced Topics

### Rate Limiting

Rate limiting protects your API from abuse and ensures fair usage among clients. This API implements **global rate limiting** that applies to all `/api/**` endpoints using Resilience4j's token bucket algorithm.

#### How It Works

The global rate limiter uses a **token bucket** algorithm:
- **Tokens**: Each request consumes 1 token
- **Bucket Capacity**: 1000 tokens per minute (configurable)
- **Refill Rate**: Tokens refill at the end of each minute
- **Global Scope**: Single limit shared across all endpoints and clients

#### Configuration

Rate limiting is configured in `application.yml`:

```yaml
resilience4j:
  ratelimiter:
    instances:
      global-api:
        registerHealthIndicator: true
        limitRefreshPeriod: 1m              # Refill period (1 minute)
        limitForPeriod: 1000                # Requests per refill period
        timeoutDuration: 5s                 # Timeout waiting for available token
        eventConsumerBufferSize: 100
        allowHealthIndicatorToFail: false
```

**Configuration Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `limitRefreshPeriod` | 1m | Time interval to refill tokens |
| `limitForPeriod` | 1000 | Number of tokens available per period |
| `timeoutDuration` | 5s | Maximum wait time for a token |
| `registerHealthIndicator` | true | Register with Spring Boot health checks |

#### Implementation Details

**Global Interceptor** (`GlobalRateLimitInterceptor.java`):
- Intercepts all `/api/**` requests
- Attempts to acquire a permit from the rate limiter
- Returns 429 (Too Many Requests) if limit exceeded
- Adds `X-Rate-Limit-Available-Permits` header to responses
- Excludes: Swagger UI, API docs, H2 console, actuator endpoints

**Web Configuration** (`WebConfig.java`):
- Registers the interceptor with Spring MVC
- Specifies path patterns to apply rate limiting
- Configures exclusions for non-API endpoints

#### Rate Limit Exceeded Response

When rate limit is exceeded, the API returns HTTP 429:

```json
HTTP/1.1 429 Too Many Requests

{
  "status": 429,
  "message": "Too Many Requests",
  "details": "Rate limit exceeded. Please try again later."
}
```

Response header indicates available permits:
```
X-Rate-Limit-Available-Permits: 0
```

#### Testing Rate Limits

```bash
# Make rapid requests to test rate limiting
for i in {1..1005}; do
  curl -i http://localhost:8080/api/v1/computer-systems
done

# After 1000 requests, subsequent requests will receive 429 responses
```

#### Monitoring Rate Limiting

View rate limiter metrics via Spring Boot Actuator:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Rate limiter metrics
curl http://localhost:8080/actuator/metrics/resilience4j.ratelimiter.calls
```

#### Extending Rate Limiting

##### Option 1: Adjust Global Limits

Modify `limitForPeriod` in `application.yml`:

```yaml
resilience4j:
  ratelimiter:
    instances:
      global-api:
        limitForPeriod: 2000              # Increase to 2000 requests/minute
```

##### Option 2: Add Per-Endpoint Limits

Create additional rate limiter instances for stricter limits on expensive operations:

```yaml
resilience4j:
  ratelimiter:
    instances:
      global-api:
        limitForPeriod: 1000              # Global limit
      
      expensive-operation:
        limitForPeriod: 100               # Stricter limit for heavy operations
        limitRefreshPeriod: 1m
```

Then apply to specific endpoints:

```java
@PostMapping
@RateLimiter(name = "expensive-operation")
public ResponseEntity<ComputerSystemDto> createComputerSystem(@Valid @RequestBody ComputerSystemDto dto) {
    // Limited to 100 requests/minute (stricter than global)
    return ResponseEntity.status(HttpStatus.CREATED).body(computerSystemService.createComputerSystem(dto));
}
```

##### Option 3: Client-Based Rate Limiting

Implement different limits per client type (requires authentication):

```java
public class ClientAwareRateLimitInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientId = request.getHeader("X-Client-ID");
        String tier = getClientTier(clientId);  // premium, standard, free
        
        String rateLimitName = tier.equals("premium") ? "premium-api" : "standard-api";
        RateLimiter limiter = registry.rateLimiter(rateLimitName);
        
        if (limiter.acquirePermission()) {
            return true;
        }
        response.setStatus(429);
        return false;
    }
}
```

#### Best Practices

1. **Set Realistic Limits**: Base on expected traffic and server capacity
2. **Monitor Violations**: Track rate limit rejections to detect abuse
3. **Client Communication**: Provide clear error messages and retry guidance
4. **Exclude Healthchecks**: Don't rate limit monitoring and health endpoints
5. **Graceful Degradation**: Return helpful information in 429 responses
6. **Documentation**: Inform API clients about rate limits
7. **Testing**: Test behavior under rate limit conditions
8. **Combine with Circuit Breaker**: See Circuit Breaker Pattern section for downstream service protection

---

### Request/Response Compression

Compression reduces the size of data transmitted between client and server using GZIP, improving performance especially for large responses.

#### Configuration

Compression is enabled in `application.yml`:

```yaml
server:
  compression:
    enabled: true
    min-response-size: 1024                # Only compress responses > 1KB
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
```

#### How It Works

1. Client sends: `Accept-Encoding: gzip`
2. Server compresses response body with GZIP
3. Server sends: `Content-Encoding: gzip`
4. Client automatically decompresses

#### Performance Benefits

| Metric | Uncompressed | Compressed | Improvement |
|--------|------------|-----------|-------------|
| **Response Size** | 45 KB | 6.8 KB | 85% smaller |
| **Transfer Time (3G)** | 1.2s | 0.18s | 6.7x faster |
| **CPU Usage** | Baseline | +2% | Negligible |

#### Testing Compression

```bash
# Check compression is working
curl -i -H "Accept-Encoding: gzip" http://localhost:8080/api/v1/computer-systems

# Response should include:
# Content-Encoding: gzip
```

#### Best Practices

- Enable for text-based formats (JSON, XML, HTML)
- Disable for already-compressed formats (images, videos, archives)
- Set appropriate `min-response-size` threshold
- Monitor CPU impact in production

---

### Circuit Breaker Pattern

The circuit breaker pattern prevents cascading failures when external services become unavailable or slow. It acts as a "circuit breaker" - quickly failing rather than waiting for timeouts.

#### Use Cases

Circuit breakers are essential when your API depends on external services:

1. **Email Notifications**: Email server down → fail fast, log locally instead of timeout
2. **Database Connection Pool**: Database slow/unavailable → return graceful error with empty results
3. **External APIs**: Third-party service unavailable → return default response
4. **Active Directory / LDAP**: Auth service down → use local authentication fallback
5. **Message Queues**: Queue service unavailable → store locally and retry

**In this project**, circuit breakers protect:
- **Email Service**: SMTP server timeouts won't block API responses
- **Database Queries**: Connection pool exhaustion won't hang requests

#### How It Works

Circuit breaker has **3 states**:

```
CLOSED (Normal Operation)
├─ Requests pass through normally
├─ Failures counted
└─ After threshold (50% failures OR 40% slow calls) → OPEN

OPEN (Failing Fast)
├─ All requests rejected immediately
├─ Fallback method called instead
├─ No calls to external service
└─ After wait period (30s for email, 20s for database) → HALF_OPEN

HALF_OPEN (Testing Recovery)
├─ Limited requests allowed through (3 for email, 5 for database)
├─ If successful → CLOSED (fully recovered)
└─ If fails again → OPEN (not ready yet)
```

**Benefits:**
- ✅ Fails fast (don't wait 30s for SMTP timeout)
- ✅ Protects backend from cascading failures
- ✅ Allows recovery time without load
- ✅ Returns fallback response gracefully
- ✅ Monitored via health endpoints

#### Configuration

Circuit breaker configurations are in `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      # Email service circuit breaker
      emailService:
        registerHealthIndicator: true
        minimumNumberOfCalls: 5           # Need 5 calls before deciding
        slidingWindowSize: 10             # Last 10 calls evaluated
        failureRateThreshold: 50          # 50% failures opens circuit
        slowCallRateThreshold: 50         # 50% slow calls opens circuit
        slowCallDurationThreshold: 2s     # Calls taking >2s are "slow"
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 30s      # Wait 30s before testing recovery
        automaticTransitionFromOpenToHalfOpenEnabled: true
      
      # Database query circuit breaker
      databaseQuery:
        registerHealthIndicator: true
        minimumNumberOfCalls: 8
        slidingWindowSize: 20             # More lenient for database
        failureRateThreshold: 60          # 60% failures opens
        slowCallRateThreshold: 40         # 40% slow queries open
        slowCallDurationThreshold: 3s     # Queries >3s are slow
        permittedNumberOfCallsInHalfOpenState: 5
        waitDurationInOpenState: 20s      # Shorter recovery time
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

#### Implementation Examples

**Email Service with Circuit Breaker:**

```java
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    /**
     * Circuit breaker protects email sending from SMTP timeouts.
     * If email service fails, fallback method logs locally instead.
     */
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendErrorNotificationFallback")
    public void sendErrorNotification(Exception exception, String endpoint, String details) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("admin@example.com");
        message.setSubject("Error: " + exception.getClass().getSimpleName());
        message.setText(buildEmailBody(exception, endpoint, details));
        
        mailSender.send(message);  // Can fail if email server is down
    }

    /**
     * Called when emailService circuit breaker is OPEN.
     * Email unavailable, so log notification locally instead.
     */
    public void sendErrorNotificationFallback(Exception exception, String endpoint, String details,
                                             CallNotPermittedException ex) {
        logger.warn("Email service unavailable, logging error locally");
        logger.error("Error: {}", exception.getMessage());
        logger.error("Endpoint: {}", endpoint);
        // In production: write to database, send SMS, or post to Slack
    }
}
```

**Database Queries with Circuit Breaker:**

```java
@Service
@Transactional
public class ComputerSystemService {

    private final ComputerSystemRepository repository;

    /**
     * Circuit breaker protects database queries from connection pool exhaustion.
     * If database is slow/down, fallback returns empty results gracefully.
     */
    @CircuitBreaker(name = "databaseQuery", fallbackMethod = "getAllComputerSystemsFallback")
    public Page<ComputerSystemDto> getAllComputerSystems(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToDto);
    }

    /**
     * Called when databaseQuery circuit breaker is OPEN.
     * Database unavailable, return empty page to client.
     */
    public Page<ComputerSystemDto> getAllComputerSystemsFallback(Pageable pageable,
                                                                CallNotPermittedException ex) {
        logger.error("Database unavailable, returning empty results");
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
```

**Exception Handler Integration:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex,
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service temporarily unavailable",
                "A critical service is currently unavailable. Please try again in a moment.",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
}
```

#### Monitoring

Check circuit breaker status via health endpoints:

```bash
# Check all circuit breaker states
curl http://localhost:8080/actuator/health

# Response
{
  "status": "UP",
  "components": {
    "resilience4j-circuitbreaker": {
      "status": "UP",
      "details": {
        "emailService": {
          "status": "UP",
          "details": {
            "state": "CLOSED",              # CLOSED, OPEN, or HALF_OPEN
            "failureRate": "0%",
            "slowCallRate": "0%",
            "bufferedCalls": 5,
            "failedCalls": 0,
            "successfulCalls": 5
          }
        },
        "databaseQuery": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "0%",
            "slowCallRate": "0%",
            "bufferedCalls": 8,
            "failedCalls": 0,
            "successfulCalls": 8
          }
        }
      }
    }
  }
}
```

**Key Metrics:**

| Metric | Description |
|--------|-------------|
| `state` | Current state: CLOSED, OPEN, or HALF_OPEN |
| `failureRate` | % of failed calls (triggers open at threshold) |
| `slowCallRate` | % of calls exceeding duration threshold |
| `bufferedCalls` | Number of calls in evaluation window |
| `failedCalls` | Count of failures so far |
| `successfulCalls` | Count of successes so far |

#### Best Practices

1. **Meaningful Fallbacks**: Return sensible defaults, cached data, or empty results
2. **Monitor State Changes**: Alert when circuit opens (indicates external service issue)
3. **Set Realistic Thresholds**: 
   - Email: 50% failures (less tolerant, SMTP should be reliable)
   - Database: 60% failures (more tolerant, temporary slowness OK)
4. **Combine with Rate Limiting**: Use together for complete fault tolerance
5. **Test Fallbacks**: Verify fallback methods work correctly under load
6. **Logging**: Log when circuit opens/closes for debugging and monitoring
7. **Timeout Settings**: Set reasonable timeouts to avoid long waits before circuit opens
8. **Alternative Channels**: For critical alerts (email down), use alternative notification (SMS, Slack)

---

### Batch Operations

Batch operations allow you to process multiple items in a single request with **all-or-nothing semantics**. Either all items are processed successfully or none are (transaction rollback), ensuring data consistency and preventing partial updates.

#### All-or-Nothing Semantics

The API implements strict all-or-nothing transactional processing:

**Validation Phase (Spring @Valid):**
- ALL items validated before ANY processing
- If ANY item fails validation, HTTP 400 returned
- ZERO items processed if validation fails

**Processing Phase (@Transactional):**
- All items processed in single database transaction
- If ANY item fails during processing, entire transaction rolled back
- Either ALL items processed or NONE

**Benefits:**
- **Data Consistency**: No partial updates or inconsistent state
- **Fail-Fast**: Detect problems before touching database
- **Clear Semantics**: Client knows batch either fully succeeds or fully fails
- **ACID Guarantees**: Database ensures atomicity and consistency

#### Configuration

Batch size limits are configurable in `application.yml`:

```yaml
app:
  batch:
    max-items: 100          # Maximum items per batch request
    timeout-seconds: 300    # Batch operation timeout (5 minutes)
```

**Configuration Parameters:**

| Parameter | Default | Purpose | Typical Range |
|-----------|---------|---------|---|
| `max-items` | 100 | Maximum items in single batch (DOS protection) | 10-1000 |
| `timeout-seconds` | 300 | Timeout for batch operation (seconds) | 30-600 |

**Recommendations:**
- Set `max-items` based on item complexity and memory constraints
- For simple items: 100-500
- For complex items (large payloads): 10-50
- Adjust `timeout-seconds` based on expected processing time for max batch

#### API Endpoints

Three batch operation endpoints are available:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/computer-systems/batch/create` | Create multiple items |
| PUT | `/api/v1/computer-systems/batch/update` | Update multiple items |
| DELETE | `/api/v1/computer-systems/batch/delete` | Delete multiple items |

#### Batch Create

Create multiple computer systems in a single all-or-nothing operation.

**Request:**
```http
POST /api/v1/computer-systems/batch/create
Content-Type: application/json

{
  "items": [
    {
      "hostname": "SERVER-001",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "john.doe",
      "department": "IT",
      "macAddress": "00:1A:2B:3C:4D:5E",
      "ipAddress": "192.168.1.100",
      "networkName": "PROD-NETWORK"
    },
    {
      "hostname": "SERVER-002",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "jane.smith",
      "department": "IT",
      "macAddress": "00:1A:2B:3C:4D:5F",
      "ipAddress": "192.168.1.101",
      "networkName": "PROD-NETWORK"
    }
  ]
}
```

**Success Response (HTTP 201):**
```json
{
  "items": [
    {
      "id": 1,
      "hostname": "SERVER-001",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "john.doe",
      "department": "IT",
      "macAddress": "00:1A:2B:3C:4D:5E",
      "ipAddress": "192.168.1.100",
      "networkName": "PROD-NETWORK"
    },
    {
      "id": 2,
      "hostname": "SERVER-002",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "jane.smith",
      "department": "IT",
      "macAddress": "00:1A:2B:3C:4D:5F",
      "ipAddress": "192.168.1.101",
      "networkName": "PROD-NETWORK"
    }
  ],
  "totalItems": 2,
  "successCount": 2,
  "failureCount": 0,
  "status": "SUCCESS",
  "timestamp": "2025-11-28T10:30:00"
}
```

**Validation Error (HTTP 400):**
```json
{
  "status": 400,
  "message": "Request validation failed",
  "details": "items[0].ipAddress: Invalid IP address format",
  "timestamp": "2025-11-28T10:30:00",
  "path": "/api/v1/computer-systems/batch/create"
}
```

**Key Points:**
- If validation fails, NO items are created
- If any item creation fails, all items are rolled back
- Returns detailed error information for each failed item

#### Batch Update

Update multiple computer systems in a single transaction.

**Request:**
```http
PUT /api/v1/computer-systems/batch/update
Content-Type: application/json

{
  "items": [
    {
      "id": 1,
      "hostname": "SERVER-001",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "john.doe",
      "department": "DevOps",  # Updated
      ...
    },
    {
      "id": 2,
      "hostname": "SERVER-002",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "jane.smith",
      "department": "DevOps",  # Updated
      ...
    }
  ]
}
```

**Success Response (HTTP 200):**
All items updated successfully, or HTTP 404 if any item not found (none updated).

**Use Cases:**
- **Bulk Configuration**: Update 50 servers to new department
- **Bulk Rename**: Update hostnames for multiple systems
- **Bulk Reconfig**: Update network settings across systems

**Data Consistency Example:**
```
Scenario: Update 100 servers' network config to new VLAN
Result:   All 100 updated with new config OR all kept old config
          (No partial updates where 75 have new config, 25 have old)
```

#### Batch Delete

Delete multiple computer systems with verification phase.

**Request:**
```http
DELETE /api/v1/computer-systems/batch/delete
Content-Type: application/json

{
  "items": [
    {"id": 1},
    {"id": 2},
    {"id": 3}
  ]
}
```

**Success Response (HTTP 204):** No Content

**Two-Phase Delete:**

1. **Verification Phase**: Verify all IDs exist before deleting ANY
   - If any ID not found, returns HTTP 404
   - NO items deleted if any not found

2. **Deletion Phase**: Delete all in transaction
   - If any delete fails, entire transaction rolled back
   - Either ALL deleted or NONE

**Prevents:**
- Deleting "3 of 5" requested items
- Inconsistent state where some items deleted, some not

#### Validation

Batch operations use Spring's declarative validation (`@Valid`) for comprehensive error checking:

**Field-Level Validation:**
- `@NotBlank` - Required fields (hostname, IP, MAC, etc.)
- `@Size` - String length bounds
- `@ValidIPv4Address` - Custom IP validation
- `@ValidMACAddress` - Custom MAC validation
- `@ValidHostname` - Custom hostname validation

**Batch-Level Validation:**
- `@NotEmpty` - Batch cannot be empty
- Configurable size limit (default: 100 items)

**Error Response Example:**
```json
{
  "status": 400,
  "message": "Request validation failed",
  "details": "items[0].hostname: Hostname must be valid RFC 1123 format; items[1].ipAddress: IP address must be valid IPv4 format",
  "path": "/api/v1/computer-systems/batch/create"
}
```

#### Error Handling

Common error scenarios:

| Status | Scenario | Action |
|--------|----------|--------|
| 400 | Batch empty or size exceeds limit | Reduce batch size |
| 400 | Validation fails (invalid field) | Fix invalid fields and retry |
| 409 | Duplicate detected (hostname, IP, MAC) | Use unique values |
| 404 | Item not found (batch delete/update) | Verify IDs exist |
| 503 | Service unavailable (circuit breaker) | Retry after delay |

**Key Behavior:**
- If ANY error occurs, NO items are processed
- Errors are atomic at batch level (all-or-nothing)
- Detailed error messages show which items/fields failed

#### Best Practices

1. **Validate Batch Size Before Sending**
   - Query `/actuator/health` for batch configuration (future enhancement)
   - Send batches under max-items limit

2. **Handle Timeouts**
   - Set client timeout higher than server timeout
   - For large batches, adjust `timeout-seconds` in config

3. **Idempotency**
   - Batch create is NOT idempotent (creates duplicates on retry)
   - Batch update IS idempotent (safe to retry)
   - Batch delete IS idempotent (deleting non-existent is safe)

4. **Data Consistency**
   - Use all-or-nothing for related items that must be consistent
   - Example: Create server + network config in one batch
   - Avoids: Server created but config failed

5. **Error Handling**
   - Parse detailed error response to identify problematic items
   - Fix only those items, not entire batch
   - Retry with corrected items

6. **Monitoring**
   - Monitor batch operation duration via logs
   - Log batch size and success rate
   - Alert on repeated batch failures

7. **Testing**
   - Test with max-items limit (edge case)
   - Test with validation failures to verify rollback
   - Test with partial failures during processing

---

### API Versioning Strategy

This API uses **URI path versioning** to manage different API versions. All endpoints are prefixed with version identifier (e.g., `/api/v1/`).

#### Current Implementation

All endpoints use the `/api/v1/` prefix:
```
POST /api/v1/computer-systems
GET /api/v1/computer-systems/{id}
PUT /api/v1/computer-systems/{id}
DELETE /api/v1/computer-systems/{id}
```

#### When to Create a New Version

Create a new API version for **breaking changes**:

✅ **Breaking Changes** (require new version):
- Removing or renaming response fields
- Changing field types
- Changing endpoint behavior
- Removing or changing required parameters

❌ **Non-Breaking Changes** (no new version):
- Adding optional fields
- Adding new endpoints
- Bug fixes and performance improvements
- Deprecating fields (with notice period)

#### Creating a New Version

**Step 1: Create new controller**
```java
@RestController
@RequestMapping("/api/v2/computer-systems")
public class ComputerSystemControllerV2 {
    // Reuse service layer, implement v2-specific logic
}
```

**Step 2: Create new DTO if needed**
```java
public class ComputerSystemDtoV2 {
    // New or modified fields
}
```

**Step 3: Support both versions simultaneously**
```
GET /api/v1/computer-systems       # Old version still works
GET /api/v2/computer-systems       # New version available
```

#### Best Practices

1. Support at least 2 versions simultaneously
2. Announce 6-month deprecation period before removing old versions
3. Maintain backwards compatibility within versions
4. Document all changes between versions
5. Follow semantic versioning (MAJOR.MINOR.PATCH)

---

### Spring Boot Actuator

Spring Boot Actuator exposes operational endpoints that provide insights into your running application, including health checks, metrics, and monitoring capabilities. It's essential for production deployments and observability.

#### What Actuator Provides

Actuator enables the following endpoints:

| Endpoint | Purpose | Example |
|----------|---------|---------|
| `/actuator/health` | Application and component health status | Health of database, dependencies, rate limiter |
| `/actuator/metrics` | Available metrics in the application | List of all metrics and their values |
| `/actuator/metrics/{metric}` | Specific metric details | `resilience4j.ratelimiter.calls` |
| `/actuator/prometheus` | Metrics in Prometheus format | For integration with monitoring systems |
| `/actuator/info` | Application information | Version, description, custom properties |

#### Configuration

Actuator endpoints are configured in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

**Configuration Parameters:**

| Parameter | Value | Description |
|-----------|-------|-------------|
| `exposure.include` | health,metrics,prometheus | Endpoints exposed over HTTP |
| `show-details` | always | Show detailed health information (database, rate limiter, etc.) |

#### Health Checks

Health endpoints return the overall application status and individual component health:

```bash
# Check overall health
curl http://localhost:8080/actuator/health

# Response
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "SELECT 1"
      }
    },
    "resilience4j-ratelimiter": {
      "status": "UP",
      "details": {
        "global-api": {
          "status": "UP"
        }
      }
    }
  }
}
```

**Use Cases:**
- Docker container startup verification
- Kubernetes liveness and readiness probes
- Load balancer health monitoring
- Automated alerting systems

#### Monitoring Rate Limiter Metrics

Actuator integrates with Resilience4j to expose detailed rate limiter metrics:

```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics

# Get rate limiter call metrics
curl http://localhost:8080/actuator/metrics/resilience4j.ratelimiter.calls

# Response
{
  "name": "resilience4j.ratelimiter.calls",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 950
    }
  ]
}
```

**Key Metrics:**

| Metric | Description |
|--------|-------------|
| `resilience4j.ratelimiter.calls` | Total number of calls (successful + rejected) |
| `resilience4j.ratelimiter.calls.successful` | Permitted requests |
| `resilience4j.ratelimiter.calls.rejected` | Rejected requests (429 responses) |
| `resilience4j.ratelimiter.calls.delayed` | Requests waiting for available tokens |

#### Integration with Monitoring Systems

Export metrics to Prometheus/Grafana for dashboards:

```bash
# Get metrics in Prometheus format
curl http://localhost:8080/actuator/prometheus

# Output
# HELP resilience4j_ratelimiter_calls_total Total number of calls
# TYPE resilience4j_ratelimiter_calls_total counter
resilience4j_ratelimiter_calls_total{instance="global-api"} 950
resilience4j_ratelimiter_calls_rejected_total{instance="global-api"} 5
```

**Prometheus Configuration Example:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'api-demo'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

#### Example Kubernetes Configuration

Use health endpoints for Kubernetes probes:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: api-demo
spec:
  containers:
  - name: api-demo
    image: api-demo:1.0.0
    ports:
    - containerPort: 8080
    livenessProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /actuator/health
        port: 8080
      initialDelaySeconds: 5
      periodSeconds: 5
```

#### Best Practices

1. **Expose Only Required Endpoints**: Use `exposure.include` to limit exposure
2. **Secure Actuator Endpoints**: Add authentication/authorization in production
3. **Monitor Metrics**: Set up alerts for concerning metrics (high rejection rates, etc.)
4. **Health Checks**: Use for load balancer and orchestrator integration
5. **Rate Limiter Monitoring**: Track rejection rates to detect abuse
6. **Performance Impact**: Actuator adds minimal overhead; metrics collection is efficient

---

## Recent Updates (November 2025)

### RFC 9457 ProblemDetail Migration
- **Migrated from custom `ErrorResponse` to Spring's `ProblemDetail`** (RFC 9457 standard)
- Replaced proprietary error format with industry-standard problem details format
- All error responses now follow RFC 9457 specification with fields: `type`, `title`, `status`, `detail`, `instance`, `timestamp`
- Benefits:
  - Interoperable with standard API clients and tools
  - Built into Spring Framework 6.2+ (no custom code needed)
  - Extensible with custom properties for domain-specific errors
  - Standards-based approach (recognized by Postman, REST clients, etc.)
- Updated `GlobalExceptionHandler` to return `ProblemDetail` responses
- Updated batch validation to return `ProblemDetail` with enhanced error context
- All tests updated and passing (37/37 tests)

## Observability & Metrics

This application uses **Spring Boot Actuator** and **Micrometer** for metrics and monitoring. All metrics are automatically registered on application startup and available via standard actuator endpoints.

### Accessing Metrics

#### Get All Metrics
```
GET http://localhost:8080/actuator/metrics
```

Response includes a list of all available metrics with their names.

#### Get Specific Metric Value
```
GET http://localhost:8080/actuator/metrics/app.computersystems.total
```

Response example:
```json
{
  "name": "app.computersystems.total",
  "description": "Total number of computer systems present in the system",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 42.0
    }
  ],
  "availableTags": []
}
```

#### Get Metrics in Prometheus Format
```
GET http://localhost:8080/actuator/prometheus
```

Output includes all metrics in Prometheus text format suitable for scraping:
```
app_computersystems_total 42.0
```

### Available Custom Metrics

#### `app.computersystems.total` (GAUGE)
- **Type**: Gauge (point-in-time value)
- **Description**: Total number of computer systems present in the system
- **Updates**: Real-time from database
- **Usage**: Monitor total system inventory count

### Adding New Metrics

To add a new custom metric, follow these steps:

#### 1. Create a Metric Component
Create a new component in `com.demo.shared.metrics/` that registers a gauge:

```java
@Component
public class CustomMetric {
    
    private final MeterRegistry meterRegistry;
    private final SomeRepository repository;
    
    public CustomMetric(MeterRegistry meterRegistry, SomeRepository repository) {
        this.meterRegistry = meterRegistry;
        this.repository = repository;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void registerMetrics() {
        meterRegistry.gauge(
            "app.custom.metric.name",
            repository,
            SomeRepository::countMethod  // Method reference that returns a Number
        );
    }
}
```

#### 2. Naming Convention
Use the pattern: `app.<domain>.<metric_type>.<name>`

Examples:
- `app.computersystems.total`
- `app.users.active`
- `app.orders.pending`

#### 3. Automatic Registration
The metric will be automatically discovered and registered via Spring's component scanning.

### Monitoring and Observability

#### Spring Boot Health Endpoint
```
GET http://localhost:8080/actuator/health
```

Includes health status for all managed components (database, rate limiters, circuit breakers, etc.).

#### Integrating with Monitoring Systems

**Prometheus**: Scrape the `/actuator/prometheus` endpoint at regular intervals (e.g., every 15 seconds).

**Example Prometheus Configuration**:
```yaml
scrape_configs:
  - job_name: 'api-demo'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

**Grafana**: Create dashboards that query Prometheus metrics like:
```
increase(app_computersystems_total[5m])  # 5-minute change rate
```

### Metrics Configuration

Metrics configuration is in `MetricsConfiguration.java`:
- Global tags applied to all metrics (application name, version)
- Timer support via `@Timed` annotations
- Micrometer MeterRegistry customization

All actuator endpoints are enabled in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

## Authentication & Authorization

This API implements a comprehensive, database-driven Role-Based Access Control (RBAC) system with field-level permissions.

### Overview

The RBAC system provides three layers of security:

1. **Authentication**: Validate user identity (Active Directory, LDAP, or API tokens)
2. **Object-Level Authorization**: Control which resources users can access
3. **Field-Level Authorization**: Control which fields users can read and write

Active Directory authentication uses `sAMAccountName` for login. AD group memberships map directly to roles, and users with no group membership receive the `USER` role.

For testing and local development, an **embedded LDAP server** (UnboundID) provides authentication without requiring external infrastructure. LDAP groups are mapped to application roles:

| LDAP Group | Application Role |
|---|---|
| `GroupA-Users` | `ROLE_MY_APP_USER` |
| `GroupB-Admins` | `ROLE_MY_APP_ADMIN` |
| `GroupC-SuperAdmins` | `ROLE_MY_APP_SUPERADMIN` |

Test users: `user1`/`password1`, `user2`/`password2` (Users), `admin1`/`admin123` (Users + Admins), `superadmin1`/`super123` (SuperAdmins).

### Key Features

✅ **Dynamic Role Management**: Roles defined in database, not hardcoded enums  
✅ **Field-Level Permissions**: Control field access granularly  
✅ **Permission Scopes**: OWN, DEPARTMENT, and ALL scopes  
✅ **Immutable & Read-Only Fields**: System-enforced field restrictions  
✅ **Zero-Code-Redeployment**: Changes take effect immediately via cache reload  
✅ **Permission Caching**: High-performance with in-memory caching  

### PEM Keys & BouncyCastle

- The authentication subsystem supports RSA private keys provided as PEM files.
- Supported PEM formats: PKCS#8 (`-----BEGIN PRIVATE KEY-----`) and PKCS#1
  (`-----BEGIN RSA PRIVATE KEY-----`). The code accepts either format and will
  convert PKCS#1 to a Java-compatible key at startup.
- We include the `bcprov-jdk15on` (BouncyCastle) dependency to parse/convert
  PEM files during application initialization. This avoids requiring offline
  key conversion during development and makes local testing simpler.
- If you prefer not to include BouncyCastle, convert keys to PKCS#8 with OpenSSL:

```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in examplejwtkey.pem -out examplejwtkey-pk8.pem
```

Add the resulting PKCS#8 PEM path to `security.jwt.private-key-path` or set
`JWT_PRIVATE_KEY_PATH` in the environment. In production, use a secure
keystore or secret manager and avoid storing private keys in `application.yml`.

### Architecture & Flow

```
┌──────────────┐
│   Request    │
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│ Authentication Layer │ ← Validate user identity
│ (LDAP/Active Directory/JWT) │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────┐
│ Authorization Service    │ ← Check object-level access
│  - Role lookup           │
│  - Scope checking        │
│    (OWN/DEPARTMENT/ALL)  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Field Permission Filter  │ ← Filter readable/writable fields
│  - Read permissions      │
│  - Write permissions     │
│  - Immutable fields      │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────┐
│   Response       │
└──────────────────┘
```

### Database Schema

#### Roles Table
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### Permissions Table
```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,      -- READ, WRITE, DELETE
    scope VARCHAR(50) NOT NULL,          -- OWN, DEPARTMENT, ALL
    field_permissions TEXT,              -- JSON: {"field": "READ|WRITE|HIDDEN"}
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### Role_Permissions Table (Junction)
```sql
CREATE TABLE role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);
```

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

### Default Roles

Three default roles are created on application startup:

#### MY_APP_SUPERADMIN
- **Scope**: ALL
- **Permissions**: Full access to all fields
- **Use Case**: System administrators

#### MY_APP_ADMIN
- **Scope**: DEPARTMENT
- **Permissions**: 
  - Can read all fields in their department
  - Cannot modify: systemUser, department, networkName
- **Use Case**: Department managers

#### MY_APP_USER
- **Scope**: OWN
- **Permissions**:
  - Can read all fields of their own resources
  - Cannot modify: systemUser, department, networkName, macAddress, ipAddress
- **Use Case**: Regular users

### Field Permission Configuration

Field permissions are stored as JSON in the `field_permissions` column:

```json
{
  "systemUser": "READ",     // Can read, cannot write
  "department": "READ",     // Can read, cannot write
  "networkName": "WRITE",   // Can read and write
  "id": "HIDDEN"            // Completely hidden from response
}
```

**Permission Levels**:
- `WRITE`: Can read and write
- `READ`: Can read but not write
- `HIDDEN`: Completely hidden from response

### Code Organization

The RBAC system follows a clean, modular package structure with separation of concerns:

#### Domain Layer (`com.demo.domain`)

Entity types are organized in their own packages, each containing the entity and its DTO:

```
domain/
├── computersystem/
│   ├── ComputerSystem.java          // Entity
│   └── ComputerSystemDto.java       // Data Transfer Object
├── user/
│   ├── User.java                    // Entity
│   └── UserDto.java                 // Data Transfer Object
├── security/
│   ├── Role.java                    // Entity
│   ├── Permission.java              // Entity
│   ├── RolePermission.java          // Junction Entity
│   └── dto/
│       ├── RoleDto.java
│       └── PermissionDto.java
└── batch/
    ├── BatchComputerSystemRequest.java
    └── BatchComputerSystemResponse.java
```

**Pattern**: Each domain entity type gets its own package containing both the entity and DTO, promoting modularity and making it easy to locate related files.

#### Application Layer (`com.demo.application`)

Repositories, services, and controllers are organized by domain:

```
application/
├── computersystem/
│   ├── ComputerSystemRepository.java
│   ├── ComputerSystemService.java
│   └── ComputerSystemController.java
├── user/
│   └── UserRepository.java
├── security/
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   ├── RolePermissionRepository.java
│   ├── RoleManagementService.java
│   ├── RoleManagementController.java
│   ├── EmbeddedLdapTestConfig.java   // Test-only: embedded LDAP server config
│   ├── EmbeddedLdapAuthenticationTest.java  // LDAP auth unit tests
│   └── LdapIntegrationTest.java      // LDAP integration tests
└── batch/
    └── BatchComputerSystemController.java
```

**Pattern**: Each domain gets its own package in the application layer, containing its repository, service, and controller. This maintains consistency and makes it easy to navigate the codebase.

#### Shared Layer (`com.demo.shared`)

Cross-cutting concerns like security services, exception handling, and utilities:

```
shared/
├── security/
│   ├── RolePermissionService.java         // Permission caching
│   ├── AuthorizationService.java          // Access control checks
│   ├── FieldPermissionFilterService.java  // DTO field filtering
│   ├── FieldPermissionsConfig.java        // Immutable field definitions
│   ├── RoleInitializationService.java     // Default role setup
│   ├── CustomUserPrincipal.java           // User security principal
│   ├── ApiTokenPrincipal.java             // API token principal
│   └── AuthenticationContext.java         // Thread-local auth context
└── exception/
    ├── DuplicateResourceException.java
    └── ResourceNotFoundException.java
```

### Admin APIs

All admin endpoints require SUPER_ADMIN role and are prefixed with `/api/v1/admin`.

#### Role Management

```http
# Create Role
POST /api/v1/admin/roles
Content-Type: application/json

{
  "name": "DEVELOPER",
  "description": "Developer role with code access"
}

# Get All Roles
GET /api/v1/admin/roles

# Get Role by ID
GET /api/v1/admin/roles/{id}

# Update Role
PUT /api/v1/admin/roles/{id}

# Delete Role
DELETE /api/v1/admin/roles/{id}
```

#### Permission Management

```http
# Create Permission
POST /api/v1/admin/permissions
Content-Type: application/json

{
  "resourceType": "ComputerSystem",
  "operation": "READ",
  "scope": "DEPARTMENT",
  "fieldPermissions": "{\"systemUser\":\"READ\",\"department\":\"READ\"}"
}

# Get All Permissions
GET /api/v1/admin/permissions

# Get Permission by ID
GET /api/v1/admin/permissions/{id}

# Update Permission
PUT /api/v1/admin/permissions/{id}

# Delete Permission
DELETE /api/v1/admin/permissions/{id}
```

#### Role-Permission Assignment

```http
# Assign Permission to Role
POST /api/v1/admin/roles/{roleId}/permissions/{permissionId}

# Revoke Permission from Role
DELETE /api/v1/admin/roles/{roleId}/permissions/{permissionId}

# Get Permissions for Role
GET /api/v1/admin/roles/{roleId}/permissions
```

#### User Management

```http
# Create User
POST /api/v1/admin/users
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john.doe@example.com",
  "department": "IT",
  "roleId": 1
}

# Get All Users
GET /api/v1/admin/users

# Get User by ID
GET /api/v1/admin/users/{id}

# Update User
PUT /api/v1/admin/users/{id}

# Delete User
DELETE /api/v1/admin/users/{id}
```

#### Cache Management

```http
# Reload Permissions Cache
POST /api/v1/admin/cache/reload

# Response
{
  "message": "Permissions cache reloaded successfully"
}
```

**When to Reload Cache**:
- After creating/updating/deleting roles
- After creating/updating/deleting permissions
- After assigning/revoking role permissions

Cache automatically reloads after these operations, but manual reload is available if needed.

### Step-by-Step: Adding a New Object Type

Example: Adding a "Server" resource type to the RBAC system.

#### Step 1: Define the Entity

```java
@Entity
@Table(name = "servers")
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String ipAddress;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Getters, setters, builders...
}
```

#### Step 2: Update FieldPermissionsConfig

```java
private void initializeServerFields() {
    // Immutable fields
    immutableFields.put("Server", Set.of("id", "createdAt", "createdBy"));
    
    // Read-only fields
    readOnlyFields.put("Server", Set.of("updatedAt"));
    
    // Hidden fields (role-dependent)
    hiddenFields.put("Server", new HashSet<>());
}
```

#### Step 3: Create Permissions via Admin API

```http
# Create READ permission for ADMIN role
POST /api/v1/admin/permissions
{
  "resourceType": "Server",
  "operation": "READ",
  "scope": "DEPARTMENT",
  "fieldPermissions": "{}"
}

# Create WRITE permission for ADMIN role
POST /api/v1/admin/permissions
{
  "resourceType": "Server",
  "operation": "WRITE",
  "scope": "DEPARTMENT",
  "fieldPermissions": "{\"ipAddress\":\"READ\"}"
}
```

#### Step 4: Assign Permissions to Roles

```http
# Get ADMIN role ID (assume it's 2)
GET /api/v1/admin/roles

# Assign READ permission (assume permission ID is 10)
POST /api/v1/admin/roles/2/permissions/10

# Assign WRITE permission (assume permission ID is 11)
POST /api/v1/admin/roles/2/permissions/11
```

#### Step 5: Reload Cache

```http
POST /api/v1/admin/cache/reload
```

### Step-by-Step: Adding a New Field to Existing Object

Example: Adding a "location" field to ComputerSystem.

#### Step 1: Update Entity

```java
@Entity
@Table(name = "computer_systems")
public class ComputerSystem {
    // ... existing fields ...
    
    @Column(length = 255)
    private String location;
    
    // Getter and setter
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
}
```

#### Step 2: Update DTO

```java
@Schema(description = "Computer System Data Transfer Object")
public class ComputerSystemDto {
    // ... existing fields ...
    
    @Schema(description = "Physical location", example = "Building A, Floor 3")
    private String location;
    
    // Getter and setter
}
```

#### Step 3: Decide Field Permissions

For each role, decide if the field should be:
- `WRITE` (default - can read and write)
- `READ` (read-only for this role)
- `HIDDEN` (completely hidden)

#### Step 4: Update Permissions via Admin API

```http
# Update ADMIN role's WRITE permission for ComputerSystem
PUT /api/v1/admin/permissions/{permissionId}
{
  "resourceType": "ComputerSystem",
  "operation": "WRITE",
  "scope": "DEPARTMENT",
  "fieldPermissions": "{\"systemUser\":\"READ\",\"department\":\"READ\",\"networkName\":\"READ\",\"location\":\"WRITE\"}"
}
```

#### Step 5: Reload Cache

```http
POST /api/v1/admin/cache/reload
```

### Security Best Practices

1. **Principle of Least Privilege**: Grant minimum permissions required
2. **Regular Permission Audits**: Review role permissions quarterly
3. **Field-Level Security**: Always define field permissions explicitly
4. **Cache Management**: Reload cache after permission changes
5. **Audit Logging**: Log all permission changes (future enhancement)
6. **Authentication**: Integrate with Active Directory or JWT in production; use embedded LDAP for testing
7. **API Protection**: Restrict admin endpoints to trusted networks
8. **Database Backups**: Regular backups of roles/permissions tables
9. **Testing**: Test permission changes in staging before production

### Testing Scenarios

#### Test 1: User Can Only Access Own Resources

```http
# As USER role
GET /api/v1/computer-systems

# Expected: Only sees ComputerSystems where createdBy = current user
```

#### Test 2: Admin Can Access Department Resources

```http
# As ADMIN role in IT department
GET /api/v1/computer-systems

# Expected: Sees all ComputerSystems in IT department
```

#### Test 3: Field-Level Restrictions

```http
# As USER role
PUT /api/v1/computer-systems/1
{
  "hostname": "NEW-NAME",
  "department": "HR"   // Should be rejected
}

# Expected: 403 Forbidden - Cannot modify 'department' field
```

#### Test 4: Super Admin Has Full Access

```http
# As SUPER_ADMIN role
PUT /api/v1/computer-systems/1
{
  "hostname": "NEW-NAME",
  "department": "HR"   // Should succeed
}

# Expected: 200 OK - All fields modifiable
```

### Troubleshooting Guide

#### Problem: Permission changes not taking effect

**Solution**: Reload the permissions cache:
```http
POST /api/v1/admin/cache/reload
```

#### Problem: User cannot access their own resources

**Checklist**:
1. Verify user has a role assigned
2. Check role has READ permission with scope OWN or higher
3. Verify createdBy field is set correctly on resources
4. Reload cache if permissions were recently changed

#### Problem: Field appears in response but shouldn't

**Checklist**:
1. Check field permissions JSON for the role's permission
2. Verify field is marked as HIDDEN (not READ)
3. Reload cache
4. Check FieldPermissionsConfig doesn't override setting

#### Problem: Cannot create/update role

**Possible Causes**:
1. Role name already exists (must be unique)
2. Missing required fields (name)

#### Problem: Slow permission checks

**Solutions**:
1. Verify cache is working (check logs for cache hits)
2. Consider increasing JVM heap size for larger permission sets
3. Review database indexes on role/permission tables

### Performance Considerations

- **Permission Cache**: In-memory cache avoids database queries
- **Lazy Loading**: User relationships loaded only when needed
- **Database Indexes**: Indexed on role names, user usernames
- **Field Filtering**: Happens in-memory after database fetch
- **Connection Pooling**: HikariCP connection pool (default in Spring Boot)

### Future Enhancements

- Integrate with Spring Security for authentication
- Add Active Directory integration
- Implement JWT token-based authentication
- Add audit logging for all permission checks
- Add UI for role/permission management
- Support for custom permission validators
- Implement time-based permissions (scheduled access)

## Ideas for future Enhancements

- ~~Add authentication and authorization (JWT)~~ ✅ **IMPLEMENTED: RBAC system with field-level permissions**
- Implement per-client rate limiting (API keys/authentication)
- Implement distributed rate limiting with Redis
- Add caching layer (Redis) for frequently accessed data
- Implement audit logging for all operations
- ~~Integrate Spring Security with Active Directory/JWT authentication~~ ✅ **IMPLEMENTED: Active Directory + JWT auth**
- Implement soft deletes for data recovery
- Create analytics and reporting endpoints
- Add request/response encryption for sensitive data
- Implement distributed tracing with OpenTelemetry (successor to Spring Cloud Sleuth)
- ~~Add custom metrics collection (Micrometer)~~ ✅ **IMPLEMENTED: Custom metrics via Micrometer**
- Implement database-level auditing and change tracking
- Flyway database migration

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

For support or questions, please contact support@example.com