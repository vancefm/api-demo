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
      - [Monitoring Circuit Breaker Metrics](#monitoring-circuit-breaker-metrics)
      - [Integration with Monitoring Systems](#integration-with-monitoring-systems)
      - [Example Kubernetes Configuration](#example-kubernetes-configuration)
      - [Best Practices](#best-practices-5)
  - [Recent Updates (June 2026)](#recent-updates-june-2026)
    - [Auth & Rate-Limiting Overhaul](#auth--rate-limiting-overhaul-branch-overhaulauth-features)
    - [Spring Boot 4.1 / JDK 25 Upgrade](#spring-boot-41--jdk-25-upgrade)
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
  - [Code Organization](#code-organization)
  - [Adding a New Domain Model](#adding-a-new-domain-model)
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
- **Response Compression**: GZIP compression for improved performance and bandwidth savings
- **Logging**: Comprehensive logging at service and controller levels
- **Constructor Injection**: Dependency injection via constructors
- **Database**: H2 in-memory database with JPA/Hibernate ORM
- **Testing**: Unit and integration tests with JUnit 5 and Mockito
- **Constructor Injection**: Dependency injection via constructors
- **Database**: H2 in-memory database with JPA/Hibernate ORM
- **Testing**: Unit and integration tests with JUnit 5 and Mockito

## Technologies Used

- **Spring Boot 4.1.0**: Latest stable framework
- **Spring Framework 7**: Core framework (Jakarta EE 11 baseline)
- **Java 25**: Latest LTS programming language
- **Spring Data JPA**: ORM and repository pattern
- **Hibernate ORM 7**: JPA implementation
- **H2 Database**: In-memory relational database
- **Jackson 3** (`tools.jackson`): JSON serialization (the Spring Boot 4 default)
- **SpringDoc OpenAPI 3.0.3**: Swagger/OpenAPI documentation
- **Lombok**: Reduce boilerplate code
- **JUnit 5**: Testing framework
- **Mockito 5.23.0**: Mocking framework with JDK 25 support
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
- **Spring Native**: Uses `ProblemDetail` built into Spring Framework 6.2+ (Spring Framework 7 here)
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
- Java 25 or higher (LTS version)
- Maven 3.9.0 or higher

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
- **departmentIds**: IDs of owning departments (optional, zero or more — shared ownership)
- **departments**: Owning departments as full objects (populated on read)
- **macAddress**: MAC address (unique, required, validated)
- **ipAddress**: IP address (unique, required, validated)
- **networkName**: Network name (required)

### Department Entity Fields

- **id**: Unique identifier (auto-generated)
- **name**: Department name (unique, required)
- **description**: Department description (optional)

`departments` is a registry table: it holds only the departments that exist and
carries no references back to its owners. Each owning model links to it through
its own join table (`user_departments`, `computer_system_departments`), mapped as
a join entity (`UserDepartment`, `ComputerSystemDepartment`) so both foreign keys
can declare `ON DELETE CASCADE`. Each owner may belong to zero, one, or many
departments (shared ownership). Requests reference departments by ID via
`departmentIds`; responses include both `departmentIds` and the full nested
`departments` objects.

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
  "departmentIds": [1],
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
GET /api/v1/computer-systems/filter?hostname=SERVER&departmentId=1&userId=7&page=0&size=20&sort=id,desc
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
  "departmentIds": [2],
  "macAddress": "00:1A:2B:3C:4D:5F",
  "ipAddress": "192.168.1.101",
  "networkName": "PROD-NETWORK"
}
```

### Delete Computer System
```
DELETE /api/v1/computer-systems/{id}
```

## Departments API

Full CRUD for departments at `/api/v1/departments`.

### Create Department
```
POST /api/v1/departments
Content-Type: application/json

{
  "name": "IT",
  "description": "Information Technology"
}
```

### Get All Departments (with pagination)
```
GET /api/v1/departments?page=0&size=20&sort=name,asc
```

### Get Department by ID
```
GET /api/v1/departments/{id}
```

### Filter Departments
```
GET /api/v1/departments/filter?name=IT&description=Technology&page=0&size=20&sort=name,asc
```

### Update Department
```
PUT /api/v1/departments/{id}
Content-Type: application/json

{
  "name": "IT",
  "description": "Information Technology and Operations"
}
```

### Delete Department
```
DELETE /api/v1/departments/{id}
```

> **Note:** Deleting a department is never blocked by existing assignments — any
> users or computer systems that reference it are **silently dissociated** (their
> `departmentIds`/`departments` simply shrink). There is no orphan check.
>
> Dissociation is enforced by `ON DELETE CASCADE` on each join table's
> `department_id` foreign key, not by application code, so it applies to every
> owning model automatically — including ones added later.

Department fields on users and computer systems:
- Requests (create/update) send `"departmentIds": [1, 3]` referencing existing
  departments; unknown IDs fail the request with 404. Omitting `departmentIds`
  (or sending an empty list) on an update clears all department assignments.
- Responses include both `departmentIds` and nested `departments` objects, e.g.:
```json
{
  "id": 42,
  "hostname": "SERVER-001",
  "departmentIds": [1],
  "departments": [
    {"id": 1, "name": "IT", "description": "Information Technology"}
  ]
}
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

Filtering is implemented with **JPA Specifications** (`JpaSpecificationExecutor` +
a per-feature `<Entity>Specifications` class, e.g. `ComputerSystemSpecifications`,
`UserSpecifications`, `DepartmentSpecifications`) rather than hand-written
`@Query` strings — new filter criteria are added as composable specification
methods. All filter parameters are optional; omitted parameters contribute no
predicate.

`GET /api/v1/computer-systems/filter`:
- `hostname`: Partial match
- `departmentId`: Exact match (department membership)
- `userId`: Exact match (assigned user)

`GET /api/v1/users/filter`:
- `username`: Partial match
- `email`: Partial match
- `departmentId`: Exact match (department membership)
- `managerId`: Exact match (direct reports of the manager)

`GET /api/v1/departments/filter`:
- `name`: Partial match
- `description`: Partial match

Examples:
```
GET /api/v1/computer-systems/filter?hostname=SERVER&departmentId=1&userId=7
GET /api/v1/users/filter?departmentId=1&username=john
GET /api/v1/departments/filter?name=IT
```

## Data Validation

### Validation Rules

- **hostname**: Required, must be unique
- **manufacturer**: Required
- **model**: Required
- **user**: Required
- **departmentIds**: Optional (zero or more); every ID must reference an existing department
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
- **Total Tests**: 40 (all passing ✅)
- **Repository Tests**: 7 (Data access layer - `@DataJpaTest`)
- **Service Tests**: 10 (Business logic - mocked dependencies)
- **Controller Tests**: 7 (REST endpoints - mocked services)
- **Integration Tests**: 6 (Full application - real database with `@Transactional`)
- **Batch Controller Tests**: 10 (Batch operations)

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
4. **Test Fallbacks**: Verify fallback methods work correctly under load
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
      "departmentIds": [1],
      "macAddress": "00:1A:2B:3C:4D:5E",
      "ipAddress": "192.168.1.100",
      "networkName": "PROD-NETWORK"
    },
    {
      "hostname": "SERVER-002",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "jane.smith",
      "departmentIds": [1],
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
      "departmentIds": [1],
      "departments": [{"id": 1, "name": "IT", "description": "Information Technology"}],
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
      "departmentIds": [1],
      "departments": [{"id": 1, "name": "IT", "description": "Information Technology"}],
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
      "departmentIds": [3],  # Reassigned to the DevOps department
      ...
    },
    {
      "id": 2,
      "hostname": "SERVER-002",
      "manufacturer": "Dell",
      "model": "PowerEdge R750",
      "user": "jane.smith",
      "departmentIds": [3],  # Reassigned to the DevOps department
      ...
    }
  ]
}
```

**Success Response (HTTP 200):**
All items updated successfully, or HTTP 404 if any item not found (none updated).

**Use Cases:**
- **Bulk Configuration**: Reassign 50 servers to a new department via `departmentIds`
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
| `/actuator/health` | Application and component health status | Health of database, circuit breakers, dependencies |
| `/actuator/metrics` | Available metrics in the application | List of all metrics and their values |
| `/actuator/metrics/{metric}` | Specific metric details | `resilience4j.circuitbreaker.calls` |
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
| `show-details` | always | Show detailed health information (database, circuit breakers, etc.) |

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
    "resilience4j-circuitbreaker": {
      "status": "UP",
      "details": {
        "emailService": {
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

#### Monitoring Circuit Breaker Metrics

Actuator integrates with Resilience4j to expose detailed circuit breaker metrics:

```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics

# Get circuit breaker call metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls

# Response
{
  "name": "resilience4j.circuitbreaker.calls",
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
| `resilience4j.circuitbreaker.calls` | Total number of calls |
| `resilience4j.circuitbreaker.state` | Current state (CLOSED/OPEN/HALF_OPEN) |
| `resilience4j.circuitbreaker.failure.rate` | Percentage of failed calls |
| `resilience4j.circuitbreaker.slow.call.rate` | Percentage of slow calls |

#### Integration with Monitoring Systems

Export metrics to Prometheus/Grafana for dashboards:

```bash
# Get metrics in Prometheus format
curl http://localhost:8080/actuator/prometheus

# Output
# HELP app_computersystems_total Total number of computer systems
# TYPE app_computersystems_total gauge
app_computersystems_total 42.0
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
3. **Monitor Metrics**: Set up alerts for concerning metrics (high failure rates, etc.)
4. **Health Checks**: Use for load balancer and orchestrator integration
5. **Circuit Breaker Monitoring**: Track failure rates and open-circuit events
6. **Performance Impact**: Actuator adds minimal overhead; metrics collection is efficient

---

## Recent Updates (June 2026)

### Auth & Rate-Limiting Overhaul (branch `overhaul/auth-features`)
- **Removed the authentication & authorization implementation** pending a rebuild: JWT signing/JWKS, persistent API tokens, Active Directory / embedded-LDAP login, the database-driven RBAC model (roles, permissions, role-permissions, field-level permissions), and the associated controllers, filters, and startup seeding were all deleted.
- **`SecurityConfig` is now a minimal permit-all stub** (`@EnableWebSecurity` + a single `SecurityFilterChain` that permits every request). **The API is currently open** — no login, tokens, or role checks. The Spring Security / LDAP / JWT dependencies remain declared in `pom.xml` for the rebuild.
- **`User` keeps its profile fields and `manager` relation** but lost its `role` coupling (the RBAC `Role` entity was removed).
- **Removed application-level rate limiting**: the `GlobalRateLimitInterceptor`, `WebConfig`, and the `resilience4j.ratelimiter` configuration are gone. Circuit breakers (Resilience4j) and response compression are unaffected.
- **Removed dead/half-built scaffolding** found during the cleanup: an unused `SessionStore`/`SessionRepository`, a broken `app_config.gateway` toggle, and dangling `persistent-tokens` config.

### Spring Boot 4.1 / JDK 25 Upgrade
- **Upgraded to Spring Boot 4.1.0** (Spring Framework 7, Spring Security 7.1, Hibernate ORM 7) and **JDK 25**; the compiler now targets Java 25.
- **Jackson 3 is the new default**: migrated `ObjectMapper`/`TypeReference` usage from `com.fasterxml.jackson.*` to `tools.jackson.*`.
- **Test slices are now per-technology modules** in Boot 4 and are no longer pulled in by `spring-boot-starter-test`; added `spring-boot-webmvc-test` (`@WebMvcTest`), `spring-boot-data-jpa-test` (`@DataJpaTest`), and `spring-boot-security-test` (the MockMvc ↔ `@WithMockUser` integration).
- **Removed the unused reactive `spring-cloud-starter-gateway` dependency**: no routes were configured, it was disabled/excluded in tests, and it pulled the whole Spring Cloud release train into the build for no functional benefit. Rate limiting and circuit breaking remain provided by Resilience4j.
- Dependency bumps: SpringDoc OpenAPI `3.0.3`, Resilience4j `2.4.0` (`resilience4j-spring-boot4`), nimbus-jose-jwt `10.9` (aligned with Spring Security 7.1).
- `DaoAuthenticationProvider` now uses constructor injection (the no-arg constructor + `setUserDetailsService` were removed in Spring Security 7).

### RFC 9457 ProblemDetail Migration
- **Migrated from custom `ErrorResponse` to Spring's `ProblemDetail`** (RFC 9457 standard)
- Replaced proprietary error format with industry-standard problem details format
- All error responses now follow RFC 9457 specification with fields: `type`, `title`, `status`, `detail`, `instance`, `timestamp`
- Benefits:
  - Interoperable with standard API clients and tools
  - Built into Spring Framework 6.2+ / 7 (no custom code needed)
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

Includes health status for all managed components (database, circuit breakers, etc.).

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

## Code Organization

The codebase is organized **by feature** rather than by technical layer: each business
capability owns its entity, DTO, mapper, repository, service, and controller in a single
package, instead of being split across separate domain/application packages.

#### Feature packages (`com.demo.feature`)

```
feature/
├── user/
│   ├── User.java, UserDto.java, UserMapper.java
│   ├── UserDepartment.java                 // join entity → department
│   └── UserRepository.java, UserManagementService.java, UserManagementController.java
├── department/
│   ├── Department.java, DepartmentDto.java, DepartmentMapper.java
│   ├── DepartmentLinks.java                // reconciles an owner's link collection
│   ├── DepartmentSpecifications.java       // incl. assignedToDepartment, shared by owners
│   └── DepartmentRepository.java, DepartmentService.java, DepartmentController.java
├── computersystem/
│   ├── ComputerSystem.java, ComputerSystemDto.java, ComputerSystemMapper.java
│   ├── ComputerSystemDepartment.java       // join entity → department
│   ├── ComputerSystemRepository.java, ComputerSystemService.java, ComputerSystemController.java
│   ├── ComputerSystemMetrics.java
│   └── batch/
│       ├── BatchComputerSystemRequest.java, BatchComputerSystemResponse.java
│       ├── BatchComputerSystemController.java
│       └── BatchProperties.java
└── security/
    └── config/           — SecurityConfig (minimal permit-all stub; auth pending overhaul)
```

> **Note:** Authentication and authorization were removed pending an overhaul (see the
> `overhaul/auth-features` branch). The `security` package currently holds only a minimal
> `SecurityConfig` that permits all requests; the API is open. The Spring Security / LDAP /
> JWT dependencies remain declared in `pom.xml` for the rebuild.

**Pattern**: entities/DTOs may reference across features directly (e.g. `ComputerSystem`
referencing `User`), but repositories and internal helpers should stay package-private —
cross-feature access should go through a feature's public service methods, not its
repository.

#### Integration layer (`com.demo.integration`)

Outbound adapters to external systems, kept out of the feature packages so the transport
(HTTP client, AMQP, SMTP, etc.) can change without touching feature code:

```
integration/
└── mail/
    ├── EmailNotificationService.java   // outbound admin-alert email
    └── NoOpMailConfig.java             // no-op fallback bean
```

As HTTP clients or RabbitMQ producers/listeners are added, they follow the same rule:
colocate with the feature that owns the integration by default (e.g. `feature/x/client/`),
and only add to `integration/` when a piece of infra is genuinely shared across features
(e.g. a common `RestClient.Builder` or the AMQP connection/exchange config).

#### Platform layer (`com.demo.platform`)

Renamed from `shared` — genuinely generic, feature-agnostic infrastructure only:

```
platform/
├── BaseEntity.java                        // id, createdAt, updatedAt auditing
├── config/
│   ├── OpenApiConfig.java, MetricsConfiguration.java, JpaConfig.java
└── exception/
    ├── DuplicateResourceException.java, ResourceNotFoundException.java
    └── ErrorResponse.java, GlobalExceptionHandler.java
```

## Adding a New Domain Model

Checklist for contributors (human or AI) adding a new entity/feature. The structural
parts are hard to miss; the **cross-feature hooks** are the ones that silently break
things when forgotten.

### Structure

1. Create a feature package `com.demo.feature.<name>` containing the entity, DTO,
   mapper, repository, service, and controller (see [Code Organization](#code-organization)).
2. Entity extends `BaseEntity` (id + audit timestamps). Collection-valued
   associations use `Set` with `@Builder.Default` initialization.
3. DTO carries `@Schema` documentation and Jakarta validation annotations. For
   associations, follow the ids-in/objects-out convention: requests send
   `<x>Ids`, responses populate both `<x>Ids` and read-only nested `<x>` DTOs
   (see the `departmentIds`/`departments` pair on `ComputerSystemDto`).
4. Controller lives under `/api/v1/...` with OpenAPI annotations and
   `@PageableDefault` pagination on list endpoints.
5. Dynamic filtering uses Specifications: extend `JpaSpecificationExecutor` and
   add an `<Entity>Specifications` class — do not add `@Query` filter methods.

### Associating a new model with departments

Nothing needs registering in the department feature — not a call, not a list, not
an enum. Deletion is enforced by foreign keys, so a new owner type cannot be
forgotten. Copy the pattern from `ComputerSystemDepartment` / `UserDepartment`:

1. **Join entity** `<Owner>Department extends BaseEntity` in the *owner's* package,
   mapped to a `<owner>_departments` table with two `@ManyToOne`s — one to the
   owner, one to `Department` — and `@OnDelete(action = OnDeleteAction.CASCADE)`
   on **both**. Add a `@UniqueConstraint` on the id pair and an index on
   `department_id`. The join row is an entity rather than an implicit
   `@ManyToMany` join table for a specific reason: `@OnDelete` on a many-to-many
   collection only reaches the FK pointing at the owning table, so cascading the
   *department* side would otherwise need a hand-written DDL string.
2. **Collection on the owner**: `@OneToMany(mappedBy = "...", cascade = ALL,
   orphanRemoval = true)` plus `@BatchSize(size = 50)`, named exactly
   `departmentLinks` — `DepartmentSpecifications.assignedToDepartment` resolves
   that name. `orphanRemoval` means link changes flush with the owner, so the
   join entity needs no repository of its own.
3. **Create/update**: `DepartmentLinks.replace(...)` reconciles the collection
   against the requested IDs (see `ComputerSystemService.setDepartmentLinks`). It
   diffs rather than rebuilding, so surviving links keep their audit timestamps.
4. **Delete**: nothing to do — the owner-side `ON DELETE CASCADE` removes links.
5. **Filter**: delegate the `departmentId` predicate to
   `DepartmentSpecifications.assignedToDepartment(departmentId)`; do not write
   another `inDepartment` specification.

> **Careful:** the cascades exist only because Hibernate generates the schema
> (`ddl-auto`). Adopting Flyway means writing `ON DELETE CASCADE` into the
> migrations, or dissociation silently stops working — `DepartmentCascadeIT`
> asserts `DELETE_RULE = CASCADE` straight out of `INFORMATION_SCHEMA` to catch
> exactly that.

### Fetching associations: entity graphs vs. `@BatchSize`

One rule, and getting it backwards is a silent performance bug rather than a
failure:

- **Single-entity reads** (`findById`, `findByHostname`) → `@EntityGraph`, including
  the collection paths.
- **Paged reads** (`findAll(Pageable)`, `findAll(Specification, Pageable)`) → graph
  only to-one associations. A collection-fetching graph combined with pagination
  makes Hibernate join the collection and paginate **in memory** (`HHH000104`),
  loading the entire result set. Collections are batched instead, via
  `@BatchSize` on the collection itself.

`ComputerSystemDepartmentFetchIT` pins this by asserting the query count for a
page does not grow with page size.

### Cross-feature hooks (easy to forget)

- **Resolve associations through services, not repositories**: cross-feature ID
  resolution goes through the owning feature's public service (e.g.
  `DepartmentService.resolveDepartments`), which owns the 404-on-unknown-ID
  behavior. Never inject another feature's repository for reads.
- **Duplicate/uniqueness checks** in the service throw
  `DuplicateResourceException` / `ResourceNotFoundException` so
  `GlobalExceptionHandler` maps them to RFC 9457 responses.

### Tests & docs

- Add tests at all four layers: repository `@DataJpaTest`, service unit test,
  controller `@WebMvcTest`, and full integration test (see
  [Test Architecture](#test-architecture)).
- If the model associates to departments, extend `DepartmentCascadeIT` to cover
  its join table in both directions (deleting the department, deleting the owner).
  There is nothing to assert at the unit level — dissociation is the database's job.
- Update this README: entity fields, endpoint examples, and test counts.
- Optional: custom metrics (`app.<domain>.<name>`, see
  [Adding New Metrics](#adding-new-metrics)) and circuit breaker protection for
  the service if it fits the pattern used by `ComputerSystemService`.

## Ideas for future Enhancements

- Rebuild authentication and authorization (overhaul in progress on `overhaul/auth-features`)
- Add caching layer (Redis) for frequently accessed data
- Implement audit logging for all operations
- Implement soft deletes for data recovery
- Create analytics and reporting endpoints
- Add request/response encryption for sensitive data
- Implement distributed tracing with OpenTelemetry (successor to Spring Cloud Sleuth)
- ~~Add custom metrics collection (Micrometer)~~ ✅ **IMPLEMENTED: Custom metrics via Micrometer**
- Implement database-level auditing and change tracking
- Flyway database migration — the join tables' `ON DELETE CASCADE` foreign keys
  must be written into the migrations; department dissociation depends on them

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Support

For support or questions, please contact support@example.com