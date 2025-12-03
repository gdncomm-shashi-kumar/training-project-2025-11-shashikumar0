# Member Service

E-Commerce Member Service - Member Profile Management

## Overview

The Member Service is a microservice built with Spring Boot that handles member profile management for an e-commerce platform. It relies on an API Gateway for authentication and authorization, focusing on managing member details and preferences.

## Features

- ✅ **Member Profile Management** - CRUD operations for member profiles
- ✅ **Redis Caching** - Caching for member data for high performance
- ✅ **Exception Handling** - Global exception handler with standardized error responses
- ✅ **Logging** - Structured logging with SLF4J and correlation IDs (traceId)
- ✅ **OpenAPI Documentation** - Interactive Swagger UI
- ✅ **Docker Support** - Multi-stage Dockerfile and docker-compose
- ✅ **Health Checks** - Spring Boot Actuator endpoints
- ✅ **Transactional Support** - Database transactions with @Transactional

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **PostgreSQL** - Member data storage
- **Redis** - Caching
- **Docker** - Containerization
- **Maven** - Build tool

## Architecture

```
┌─────────────────────────────────────────────────┐
│           Member Service API                     │
├─────────────────────────────────────────────────┤
│  Controllers (REST + OpenAPI)                    │
│  └─ MemberController (Profile management)       │
├─────────────────────────────────────────────────┤
│  Service Layer (@Transactional + Caching)       │
│  └─ MemberService                                │
├─────────────────────────────────────────────────┤
│  Data Layer                                      │
│  ├─ PostgreSQL (Member entities)                │
│  └─ Redis (Caching)                              │
└─────────────────────────────────────────────────┘
```

## API Endpoints

### Member Management Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/members/{id}` | Get member by ID | Yes (Gateway) |
| PUT | `/api/v1/members/{id}` | Update member | Yes (Gateway) |
| DELETE | `/api/v1/members/{id}` | Delete member (admin only) | Yes (Gateway) |

### Documentation & Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/swagger-ui.html` | Interactive API documentation |
| GET | `/v3/api-docs` | OpenAPI specification |
| GET | `/actuator/health` | Health check endpoint |

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- Docker and Docker Compose (for containerized setup)

### Running with Docker Compose (Recommended)

1. **Start all services:**
   ```bash
   docker-compose up -d
   ```

2. **Check service status:**
   ```bash
   docker-compose ps
   ```

3. **View logs:**
   ```bash
   docker-compose logs -f member-service
   ```

4. **Access the application:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Health Check: http://localhost:8080/actuator/health

5. **Stop services:**
   ```bash
   docker-compose down
   ```

### Running Locally

1. **Start dependencies (PostgreSQL, Redis):**
   ```bash
   docker-compose up -d postgres redis
   ```

2. **Build the application:**
   ```bash
   ./mvnw clean package
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/memberdb
spring.datasource.username=postgres
spring.datasource.password=postgres

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000 # 1 hour
```

## API Usage Examples

### Get Member Details

```bash
curl -X GET http://localhost:8080/api/v1/members/{memberId} \
  -H "Authorization: Bearer {accessToken}"
```

### Update Member

```bash
curl -X PUT http://localhost:8080/api/v1/members/{memberId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name"
  }'
```

## Error Handling

All errors follow a standardized format:

```json
{
  "timestamp": "2025-12-01T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Member not found",
  "path": "/api/v1/members/123",
  "traceId": "abc-123-def"
}
```

## Caching Strategy

- **Member Cache**: 1 hour TTL

## Logging

The service uses SLF4J with structured logging and correlation IDs:

```
2025-12-01 10:30:00.123 [http-nio-8080-exec-1] [abc-123-def] INFO  c.b.g.m.controller.MemberController - Get member request: memberId=...
```

Each request includes a `traceId` in the `X-Trace-Id` header for distributed tracing.

## Monitoring

Health check endpoint provides status of:
- Application
- PostgreSQL database
- Redis cache

Access at: `http://localhost:8080/actuator/health`

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/blibli/gdn/memberService/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST controllers
│   │   ├── domain/         # JPA entities
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Custom exceptions
│   │   ├── filter/         # Logging filter
│   │   ├── repository/     # Data repositories
│   │   └── service/        # Business logic
│   └── resources/
│       └── application.properties
└── test/
    └── java/               # Test classes
```

## Testing

Run tests:
```bash
./mvnw test
```

Run integration tests:
```bash
./mvnw verify
```

## License

Apache 2.0

## Support

For issues and questions, please contact: support@gdn.com
