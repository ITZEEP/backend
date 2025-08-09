ㄴ# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ITZeep (잇집) Backend - A Spring Framework-based real estate contract management system with fraud detection capabilities, real-time chat, and document analysis features.

## Tech Stack

- **Framework**: Spring Framework 5.3.39 (traditional WAR deployment, not Spring Boot)
- **Security**: Spring Security 5.8.15 with JWT authentication
- **Databases**: 
  - MySQL 8.x with MyBatis 3.5.16 for relational data
  - MongoDB 4.6.x for document storage (contracts, chat messages)
  - Redis 3.9.x for caching and session management
- **Real-time**: WebSocket with STOMP for chat functionality
- **Cloud Services**: AWS S3 for file storage
- **Message Queue**: Apache Kafka for async processing
- **Build Tool**: Gradle 7.x with Spotless for code formatting

## Development Commands

### Building and Running

```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport

# Apply code formatting (automatically runs before compile)
./gradlew spotlessApply

# Build WAR file for deployment
./gradlew war

# Start local development environment (Docker)
# On Windows:
start-local.bat

# View Docker logs
docker compose -f docker-compose.local.yml logs -f

# Stop Docker containers
docker compose -f docker-compose.local.yml down
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "org.scoula.domain.fraud.service.FraudRiskServiceTest"

# Run tests with detailed output
./gradlew test --info

# Generate test coverage report (HTML report in build/reports/jacoco/test/html)
./gradlew jacocoTestReport
```

### Code Quality

```bash
# Check code formatting
./gradlew spotlessCheck

# Apply code formatting fixes
./gradlew spotlessApply

# Run all quality checks (includes test coverage verification)
./gradlew check
```

## Architecture Overview

### Layer Structure

```
org.scoula/
├── domain/           # Business domains (bounded contexts)
│   ├── chat/        # Real-time chat with WebSocket
│   ├── fraud/       # Fraud risk analysis with AI integration
│   ├── home/        # Property management
│   ├── mypage/      # User profile and settings
│   ├── precontract/ # Contract preparation workflow
│   ├── user/        # User management
│   └── verification/# Identity verification
│
└── global/          # Cross-cutting concerns
    ├── auth/        # JWT authentication & Spring Security
    ├── common/      # Shared utilities, exceptions, DTOs
    ├── config/      # Application configuration
    ├── email/       # Email service
    ├── file/        # S3 file storage
    ├── mongodb/     # MongoDB configuration
    ├── oauth2/      # OAuth2 (Kakao) integration
    ├── redis/       # Redis caching
    └── websocket/   # WebSocket configuration
```

### Key Architectural Patterns

1. **Interface-Implementation Pattern**: Every controller and service has an interface definition
   - Example: `ChatController` (interface) → `ChatControllerImpl` (implementation)
   - This enables easy mocking for tests and dependency injection

2. **Multi-Database Strategy**:
   - **MySQL/MyBatis**: For structured data (users, properties, contracts)
   - **MongoDB**: For flexible documents (chat messages, contract documents)
   - **Redis**: For caching and real-time data

3. **Error Handling Architecture**:
   - Domain-specific error codes (e.g., `FraudErrorCode`, `ChatErrorCode`)
   - Global exception handler (`@RestControllerAdvice`)
   - Consistent `ApiResponse<T>` wrapper for all endpoints

4. **Authentication Flow**:
   - JWT tokens with access/refresh pattern
   - Custom authentication filters in security chain
   - Token blacklisting for logout functionality

## Critical Security Configuration

**⚠️ IMPORTANT**: The current `SecurityConfig.java` has a critical issue at line 115-116:
```java
.anyRequest().permitAll()  // This makes ALL endpoints public!
```
This should be `.authenticated()` to require authentication by default.

## Database Access Patterns

### MyBatis Mappers
- XML mapper files in `src/main/resources/org/scoula/domain/*/mapper/*.xml`
- Java mapper interfaces in `domain/*/mapper/*Mapper.java`
- Automatic underscore to camelCase mapping enabled

### MongoDB Repositories
- Spring Data MongoDB repositories for document storage
- Documents stored in `domain/*/document/*Document.java`
- Repositories in `domain/*/repository/*Repository.java`

## External Service Integration

### AI Services (Claude API)
- `AiClauseImproveService`: Contract clause improvement
- `AiContractAnalyzerService`: Contract document analysis
- `AiFraudAnalyzerService`: Fraud risk detection
- `AiDocumentAnalyzerService`: Document parsing and analysis

### AWS S3
- File uploads handled by `S3ServiceImpl`
- Configuration in `S3Config`
- Support for profile images and contract documents

### OAuth2 (Kakao)
- Manual OAuth2 implementation (not Spring OAuth2)
- Callback endpoint: `/api/auth/oauth/callback`
- Token exchange and user creation flow

## WebSocket/Real-time Features

- STOMP protocol over WebSocket for chat
- Endpoints: `/ws` for connection, `/topic/*` for subscriptions
- Chat messages persisted to MongoDB
- Bad word filtering implemented

## Configuration Management

### Properties Files
Properties are loaded from git submodule (`config-submodule/`):
- `application.properties`
- `application-default.properties`
- `application-dev.properties`
- `application-prod.properties`
- `application-secret.properties`

Run `./gradlew copyProperties` to sync from submodule.

### Environment Variables (Docker)
Key environment variables for local development:
- `DATABASE_URL`: MySQL connection string
- `REDIS_HOST`, `REDIS_PORT`: Redis configuration
- `JWT_SECRET`: JWT signing key
- `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`: AWS credentials
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`: OAuth2 credentials

## API Documentation

Swagger UI available at: `http://localhost:8080/swagger-ui.html`

## Commit Convention

```
✨ feat: 기능 추가, 삭제, 변경
🐛 fix: 버그, 오류 수정
📝 docs: 문서 수정
💄 style: UI 디자인 변경
♻ refactor: 코드 리팩토링
🧪 test: 테스트 코드
🔧 config: 설정 변경
🌱 chore: 기타 변경사항
```

## Common Development Patterns

### Adding a New API Endpoint

1. Define interface in `controller/*Controller.java`
2. Implement in `controller/*ControllerImpl.java`
3. Create service interface in `service/*ServiceInterface.java`
4. Implement service in `service/*ServiceImpl.java`
5. Define DTOs in `dto/` directory
6. Create mapper if using MyBatis
7. Add Swagger annotations for documentation
8. Write unit tests for service and controller

### Working with MyBatis

1. Create mapper interface in `mapper/*Mapper.java`
2. Create XML mapper in `resources/org/scoula/domain/*/mapper/*Mapper.xml`
3. Define VO classes for database entities
4. Use `@Mapper` annotation on interface

### Error Handling

1. Create domain-specific error code enum implementing `IErrorCode`
2. Throw `BusinessException` with appropriate error code
3. Global handler will convert to proper HTTP response

## Testing Guidelines

- Minimum coverage targets: 20% line, 10% branch (currently low, should be improved)
- Use `@SpringBootTest` for integration tests
- Use `@WebMvcTest` for controller tests
- Mock external services with `@MockBean`
- Test data builders using Lombok's `@Builder`

## Performance Considerations

- HikariCP connection pool configured for MySQL
- Redis caching available but not widely implemented
- Consider implementing `@Cacheable` for frequently accessed data
- MyBatis N+1 query issues may exist in some mappers