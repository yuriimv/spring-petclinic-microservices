# Java and Spring Boot Backend Development Skill

## Overview
Expert assistant for backend web development using modern Java and Spring Boot. Specialized in building robust, scalable microservices and REST APIs following industry best practices.

## Technology Stack
- **Java Version**: Java 25 (latest LTS features + preview features)
- **Spring Boot**: 3.5.5
- **Spring Cloud**: 2025.0.0
- **Build Tool**: Maven
- **Architecture**: Microservices

## Core Competencies

### 1. Modern Java Features (Java 25)
- **Records**: Use for immutable data carriers (DTOs, value objects)
- **Pattern Matching**: Enhanced instanceof and switch expressions
- **Text Blocks**: For SQL queries, JSON templates, multi-line strings
- **Sealed Classes**: For domain modeling with restricted hierarchies
- **Virtual Threads**: For high-concurrency scenarios (Project Loom)
- **Sequenced Collections**: Predictable ordering with SequencedCollection
- **String Templates** (Preview): Type-safe string interpolation

### 2. Spring Boot 3.x Best Practices

#### Application Structure
```
src/main/java/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── model/           # Domain entities
├── dto/             # Data transfer objects (use Records)
├── exception/       # Custom exceptions
├── security/        # Security configurations
└── validation/      # Custom validators
```

#### REST API Design
- Use `@RestController` for REST endpoints
- Implement proper HTTP methods: GET, POST, PUT, PATCH, DELETE
- Return appropriate HTTP status codes (200, 201, 204, 400, 404, 500)
- Use `ResponseEntity<T>` for fine-grained control
- Implement HATEOAS when appropriate
- Version APIs using URL versioning or headers

#### Dependency Injection
- Prefer constructor injection over field injection
- Use `@RequiredArgsConstructor` from Lombok when appropriate
- Mark single-constructor classes (auto-injection)
- Avoid `@Autowired` on fields (testability issues)

#### Configuration
- Use `application.yml` or `application.properties`
- Leverage Spring Profiles (dev, test, prod)
- Externalize configuration with `@ConfigurationProperties`
- Use `@Value` for simple property injection
- Never hardcode credentials (use environment variables)

### 3. Data Access & Persistence

#### JPA/Hibernate
- Use Spring Data JPA for repository layer
- Extend `JpaRepository<T, ID>` or `CrudRepository<T, ID>`
- Implement custom queries with `@Query` annotation
- Use entity relationships appropriately (@OneToMany, @ManyToOne, @ManyToMany)
- Apply proper fetch strategies (LAZY vs EAGER)
- Use `@Transactional` for transaction management

#### Database Best Practices
- Use Flyway or Liquibase for schema migrations
- Implement proper indexing strategies
- Use database constraints for data integrity
- Avoid N+1 query problems (use JOIN FETCH)
- Use DTOs/Projections for read operations

### 4. Microservices Patterns

#### Service Discovery
- Use Spring Cloud Netflix Eureka for service registration
- Implement proper health checks with Spring Boot Actuator
- Configure heartbeat intervals appropriately

#### API Gateway
- Use Spring Cloud Gateway for routing
- Implement rate limiting and circuit breakers
- Add request/response logging
- Configure CORS appropriately

#### Configuration Management
- Use Spring Cloud Config Server for centralized configuration
- Implement configuration refresh without restarts
- Secure sensitive configuration data

#### Resilience Patterns
- Circuit Breaker (Resilience4j)
- Retry mechanisms with exponential backoff
- Bulkhead pattern for resource isolation
- Timeouts for external calls
- Fallback strategies

### 5. Security

#### Spring Security
- Implement authentication (JWT, OAuth2, Basic Auth)
- Configure authorization with method-level security
- Use `@PreAuthorize` and `@Secured` annotations
- Implement CORS configuration properly
- Protect against CSRF, XSS, SQL Injection

#### Best Practices
- Never log sensitive data
- Use password encoders (BCrypt, Argon2)
- Implement proper session management
- Validate all input data
- Use HTTPS in production

### 6. Testing

#### Unit Testing
- Use JUnit 5 (Jupiter)
- Mock dependencies with Mockito
- Test service layer independently
- Aim for high code coverage (>80%)
- Use `@ExtendWith(MockitoExtension.class)`

#### Integration Testing
- Use `@SpringBootTest` for full context tests
- Use `@WebMvcTest` for controller tests
- Use `@DataJpaTest` for repository tests
- Use TestContainers for database integration tests
- Mock external services

#### Test Structure
```java
@DisplayName("User Service Tests")
class UserServiceTest {
    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        // When
        // Then
    }
}
```

### 7. Exception Handling

#### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

#### Custom Exceptions
- Create domain-specific exceptions
- Extend `RuntimeException` for unchecked exceptions
- Include meaningful error messages
- Log exceptions appropriately

### 8. Validation

#### Bean Validation (Jakarta)
- Use `@Valid` or `@Validated` on controller methods
- Apply constraints: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Pattern`
- Create custom validators for complex rules
- Return validation errors with proper status (400 Bad Request)

### 9. Logging

#### Best Practices
- Use SLF4J with Logback
- Implement structured logging
- Use appropriate log levels (TRACE, DEBUG, INFO, WARN, ERROR)
- Include correlation IDs for distributed tracing
- Never log sensitive information
- Use MDC (Mapped Diagnostic Context) for contextual information

```java
private static final Logger log = LoggerFactory.getLogger(MyService.class);

log.info("Processing request for user: {}", userId);
log.error("Failed to process request", exception);
```

### 10. Performance Optimization

#### Caching
- Use Spring Cache abstraction (`@Cacheable`, `@CacheEvict`)
- Choose appropriate cache provider (Caffeine, Redis)
- Set proper TTL and eviction policies
- Cache at appropriate layers

#### Async Processing
- Use `@Async` for background tasks
- Configure thread pools properly
- Use Virtual Threads (Java 25) for I/O-bound operations
- Implement proper error handling for async methods

#### Database Optimization
- Use pagination for large datasets
- Implement database connection pooling (HikariCP)
- Optimize queries with proper indexing
- Use database views for complex queries

### 11. Observability

#### Spring Boot Actuator
- Enable health checks (`/actuator/health`)
- Expose metrics (`/actuator/metrics`)
- Implement custom health indicators
- Secure actuator endpoints

#### Monitoring
- Use Micrometer for metrics collection
- Integrate with Prometheus and Grafana
- Implement distributed tracing (Spring Cloud Sleuth, Zipkin)
- Monitor JVM metrics (heap, GC, threads)

### 12. API Documentation

#### OpenAPI/Swagger
- Use SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)
- Document all endpoints with `@Operation`, `@ApiResponse`
- Provide example requests/responses
- Group APIs logically with tags
- Include authentication requirements

### 13. Build & Deployment

#### Maven
- Use Spring Boot Maven Plugin
- Implement multi-module projects when appropriate
- Configure profiles for different environments
- Use dependency management for version consistency

#### Docker
- Create optimized Dockerfile with multi-stage builds
- Use appropriate base images (eclipse-temurin, amazoncorretto)
- Implement proper health checks
- Configure resource limits

#### CI/CD
- Automate tests in pipeline
- Build Docker images automatically
- Implement blue-green or canary deployments
- Use infrastructure as code (Kubernetes, Docker Compose)

## Code Quality Standards

### General Principles
1. Follow SOLID principles
2. Write clean, readable code
3. Use meaningful variable and method names
4. Keep methods small and focused (Single Responsibility)
5. Avoid code duplication (DRY principle)
6. Write self-documenting code
7. Add comments only when necessary (explain "why", not "what")

### Code Style
- Follow Java naming conventions
- Use proper indentation (4 spaces)
- Keep line length under 120 characters
- Organize imports properly
- Use final keyword when appropriate
- Prefer composition over inheritance

### Security Checklist
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (use parameterized queries)
- [ ] XSS prevention (escape output)
- [ ] CSRF protection enabled
- [ ] Authentication and authorization implemented
- [ ] Sensitive data encrypted
- [ ] Security headers configured
- [ ] Dependencies regularly updated

## Common Patterns

### Repository Pattern
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByActiveTrue();
}
```

### Service Pattern
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}
```

### Controller Pattern
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
```

## When to Use This Skill

Use this skill when:
- Building REST APIs with Spring Boot
- Implementing microservices architecture
- Working with Spring Cloud components
- Designing database schemas and JPA entities
- Implementing authentication and authorization
- Writing tests for Spring applications
- Optimizing application performance
- Setting up CI/CD pipelines for Java applications
- Troubleshooting Spring Boot issues
- Implementing resilience patterns
- Setting up monitoring and observability

## Additional Resources

- Spring Boot Reference Documentation
- Spring Cloud Documentation
- Java 25 Release Notes
- Effective Java (Joshua Bloch)
- Spring Boot Best Practices
- Microservices Patterns (Chris Richardson)
