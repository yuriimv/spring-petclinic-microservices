# Spring PetClinic Microservices - Architectural Deep Dive

## Overview

This is a sophisticated Spring Boot 3.5.5 microservices architecture demonstrating modern cloud-native patterns. The system consists of multiple independent services coordinated through a central API Gateway, with support for distributed tracing, circuit breakers, and generative AI capabilities.

**Java Version:** 25
**Spring Cloud Version:** 2025.0.0
**Spring Boot Version:** 3.5.5

---

## 1. Service Architecture & Structure

### Services Inventory

```
Core Domain Services:
├── customers-service    (Port 8081) - Owner and Pet management
├── vets-service        (Port 8083) - Veterinarian management
├── visits-service      (Port 8082) - Visit records
└── genai-service       (Port 8084) - AI-powered interactions (Spring AI)

Infrastructure Services:
├── api-gateway         (Port 8080) - Request routing and orchestration
├── discovery-server    (Port 8761) - Eureka service registry
├── config-server       (Port 8888) - Centralized configuration
├── admin-server        (Port 9090) - Spring Boot Admin (monitoring)
├── tracing-server      (Port 9411) - Zipkin distributed tracing
├── prometheus-server   (Port 9091) - Metrics collection
└── grafana-server      (Port 3030) - Metrics visualization
```

### Package Organization Pattern

Each service follows a **layered hexagonal architecture**:

```
src/main/java/org/springframework/samples/petclinic/{service}/
├── {Service}Application.java        // Main Spring Boot entry point
│   ├── @EnableDiscoveryClient       // Register with Eureka
│   └── @SpringBootApplication       // Spring Boot auto-configuration
├── model/                           // JPA entities & repositories
│   ├── *Repository.java             // Spring Data JPA interfaces
│   └── *.java                       // @Entity domain classes
├── web/                             // REST controllers & mappers
│   ├── *Resource.java               // @RestController endpoints
│   ├── *Request.java                // DTO for requests
│   └── mapper/                      // MapStruct-style mappers
├── config/                          // Configuration classes
│   ├── MetricConfig.java            // Micrometer metrics setup
│   ├── CacheConfig.java             // Cache configuration
│   └── *Properties.java             // @ConfigurationProperties
└── dto/                             // Data transfer objects
```

---

## 2. Service Communication Patterns

### Discovery & Load Balancing

**Service Discovery:** Netflix Eureka  
- Each service registers itself with `@EnableDiscoveryClient`
- Eureka Server (`discovery-server`) maintains the service registry
- Health checks configured in docker-compose.yml

**Load Balancing:** Spring Cloud LoadBalancer  
- Services use `@LoadBalanced` WebClient.Builder for inter-service calls
- Service names resolved from Eureka registry (e.g., `lb://customers-service`)
- Used in API Gateway and GenAI service for service-to-service communication

### REST API Communication

#### 1. **API Gateway Pattern** (`api-gateway`)

The gateway implements sophisticated routing:

```yaml
# application.yml routing configuration
routes:
  - id: customers-service
    uri: lb://customers-service
    predicates:
      - Path=/api/customer/**
    filters:
      - StripPrefix=2
      - CircuitBreaker=...
```

**Gateway Features:**
- Strip path prefixes for clean internal URLs
- Apply default circuit breaker to all routes
- Per-route circuit breaker configuration (e.g., GenAI service)
- Retry policy: 1 retry on SERVICE_UNAVAILABLE for POST requests

**Gateway Implementation** (`ApiGatewayApplication`):
```java
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> 
            new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(
                    TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build()
                )
                .build());
    }
}
```

#### 2. **Service Clients Pattern** (Reactive WebClient)

Services communicate via reactive clients with load balancing:

```java
// CustomersServiceClient in API Gateway
@Component
public class CustomersServiceClient {
    private final WebClient.Builder webClientBuilder;
    
    public Mono<OwnerDetails> getOwner(final int ownerId) {
        return webClientBuilder.build().get()
            .uri("http://customers-service/owners/{ownerId}", ownerId)
            .retrieve()
            .bodyToMono(OwnerDetails.class);
    }
}
```

**Key Pattern:** Service names are resolved from Eureka (e.g., `http://customers-service/...`)

### REST Endpoints by Service

**Customers Service** (`/api/customer/**` → `/owners`)
```
POST   /owners                 - Create owner
GET    /owners                 - List all owners
GET    /owners/{ownerId}       - Get owner by ID
PUT    /owners/{ownerId}       - Update owner
GET    /owners/{ownerId}/pets  - List owner's pets
POST   /owners/{ownerId}/pets  - Create pet
```

**Vets Service** (`/api/vet/**` → `/vets`)
```
GET    /vets                   - List all veterinarians (cached)
```

**Visits Service** (`/api/visit/**` → `/`)
```
POST   /owners/*/pets/{petId}/visits        - Create visit
GET    /owners/*/pets/{petId}/visits        - Get pet's visits
GET    /pets/visits?petId={id1,id2,...}    - Batch fetch visits
```

**GenAI Service** (`/api/genai/**`)
```
POST   /chatclient             - Chat with AI assistant
```

---

## 3. Configuration Management

### Config Server Pattern

**Centralized Configuration Server** (`config-server:8888`)

```java
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**Configuration Sources:**
```yaml
# server.port: 8888
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/spring-petclinic/spring-petclinic-microservices-config
          default-label: main
        native:  # File system backend (enabled with 'native' profile)
          searchLocations: file:///${GIT_REPO}
```

### Client Configuration Import

All services use the same pattern to fetch config:

```yaml
spring:
  application:
    name: customers-service
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888/}
```

**Profile-Specific Configuration:**
- `default` - In-memory H2 database
- `docker` - Points to config-server at `http://config-server:8888`
- `postgresql` - PostgreSQL database (used in Docker)
- `production` - Enables caching (vets service)

### Property Override Examples

```yaml
# customers-service/application.yml
spring:
  application:
    name: customers-service
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888/}

# Docker profile
---
spring:
  config:
    activate:
      on-profile: docker
    import: configserver:http://config-server:8888  # Required in Docker

# PostgreSQL profile
---
spring:
  config:
    activate:
      on-profile: postgresql
  datasource:
    url: jdbc:postgresql://localhost:5432/petclinic
    username: petclinic
    password: petclinic
```

---

## 4. Database Access Patterns

### JPA Repository Pattern

All services use **Spring Data JPA** with simple repository interfaces:

```java
// OwnerRepository - minimal interface
public interface OwnerRepository extends JpaRepository<Owner, Integer> { }

// PetRepository - with custom queries
public interface PetRepository extends JpaRepository<Pet, Integer> {
    @Query("SELECT ptype FROM PetType ptype ORDER BY ptype.name")
    List<PetType> findPetTypes();
    
    @Query("FROM PetType ptype WHERE ptype.id = :typeId")
    Optional<PetType> findPetTypeById(@Param("typeId") int typeId);
}

// VisitRepository - with batch query
public interface VisitRepository extends JpaRepository<Visit, Integer> {
    List<Visit> findByPetId(Integer petId);
    List<Visit> findByPetIdIn(List<Integer> petIds);  // Batch query
}
```

### Entity Mapping

Entities use **Jakarta Persistence API** (JPA 3.1):

```java
@Entity
@Table(name = "owners")
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "first_name")
    @NotBlank
    private String firstName;
    
    // One-to-many relationship with fetch=EAGER
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<Pet> pets;
    
    public void addPet(Pet pet) {
        getPetsInternal().add(pet);
        pet.setOwner(this);
    }
}
```

### Database Initialization

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/petclinic
  sql:
    init:
      schema-locations: classpath*:db/postgresql/schema.sql
      data-locations: classpath*:db/postgresql/data.sql
      mode: always
  jpa:
    hibernate:
      ddl-auto: none  # Schema managed by SQL scripts
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

---

## 5. Testing Approach

### Test Types & Frameworks

**Unit/Integration Tests** use:
- `@WebMvcTest` - Controller layer testing with mocked repositories
- `@WebFluxTest` - Reactive controller testing
- JUnit 5 (`@ExtendWith(SpringExtension.class)`)
- Mockito for mocking (`@MockitoBean`, `given()`, `when()`)

### Testing Patterns

#### 1. Controller Tests (`@WebMvcTest`)

```java
@ExtendWith(SpringExtension.class)
@WebMvcTest(PetResource.class)  // Only loads controller & test config
@ActiveProfiles("test")  // Disable caching in tests
class PetResourceTest {
    
    @Autowired MockMvc mvc;
    @MockitoBean PetRepository petRepository;
    
    @Test
    void shouldGetAPetInJsonFormat() throws Exception {
        Pet pet = setupPet();
        given(petRepository.findById(2)).willReturn(Optional.of(pet));
        
        mvc.perform(get("/owners/2/pets/2").accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(2));
    }
}
```

#### 2. Reactive Controller Tests (`@WebFluxTest`)

```java
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = ApiGatewayController.class)
@Import({ReactiveResilience4JAutoConfiguration.class, CircuitBreakerConfiguration.class})
class ApiGatewayControllerTest {
    
    @MockitoBean
    private CustomersServiceClient customersServiceClient;
    
    @Autowired
    private WebTestClient client;
    
    @Test
    void getOwnerDetails_withAvailableVisitsService() {
        Mockito.when(customersServiceClient.getOwner(1))
            .thenReturn(Mono.just(owner));
        
        client.get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].name").isEqualTo("Garfield");
    }
    
    @Test
    void getOwnerDetails_withServiceError() {
        Mockito.when(visitsServiceClient.getVisitsForPets(...))
            .thenReturn(Mono.error(new ConnectException("Simulate error")));
        
        // Tests fallback behavior - returns empty visits
        client.get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].visits").isEmpty();
    }
}
```

#### 3. Tracing Tests

```java
// ZipkinTracingTest - verifies distributed tracing setup
// Tests that traces are properly exported to Zipkin
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific service tests
mvn -pl spring-petclinic-customers-service test

# Run specific test class
mvn test -Dtest=PetResourceTest
```

### Test Configuration

Services use a `test` profile to:
- Disable caching (`@EnableCaching` only in `production` profile)
- Use simplified configurations
- Mock external dependencies

---

## 6. Circuit Breaker & Resilience Patterns

### Resilience4j Configuration

**Framework:** Spring Cloud CircuitBreaker with Resilience4j

#### API Gateway Configuration

```java
// ApiGatewayApplication.java
@Bean
public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id -> 
        new Resilience4JConfigBuilder(id)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .timeLimiterConfig(
                TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(10))
                    .build()
            )
            .build());
}
```

#### Gateway Routes with Circuit Breakers

```yaml
# application.yml
spring:
  cloud:
    gateway:
      default-filters:
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback
        - name: Retry
          args:
            retries: 1
            statuses: SERVICE_UNAVAILABLE
            methods: POST
      routes:
        - id: genai-service
          uri: lb://genai-service
          predicates:
            - Path=/api/genai/**
          filters:
            - StripPrefix=2
            - CircuitBreaker=name=genaiCircuitBreaker,fallbackUri=/fallback
```

### Controller-Level Circuit Breakers

```java
// ApiGatewayController.java - Programmatic circuit breaker usage
@GetMapping(value = "owners/{ownerId}")
public Mono<OwnerDetails> getOwnerDetails(final @PathVariable int ownerId) {
    return customersServiceClient.getOwner(ownerId)
        .flatMap(owner ->
            visitsServiceClient.getVisitsForPets(owner.getPetIds())
                .transform(it -> {
                    ReactiveCircuitBreaker cb = cbFactory.create("getOwnerDetails");
                    return cb.run(it, throwable -> emptyVisitsForPets());  // Fallback
                })
                .map(addVisitsToOwner(owner))
        );
}

private Mono<Visits> emptyVisitsForPets() {
    return Mono.just(new Visits(List.of()));  // Fallback: empty visits
}
```

### Fallback Handling

**FallbackController** handles circuit breaker fallbacks:
```
GET /fallback  - Returns fallback response when circuit is open
```

### Resilience4j Dependencies

```xml
<!-- Circuit Breaker -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>

<!-- Reactor integration -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-reactor</artifactId>
</dependency>

<!-- Metrics integration -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
</dependency>
```

---

## 7. API Gateway Routing Implementation

### Spring Cloud Gateway Architecture

**Gateway Technology:** Spring Cloud Gateway (Reactive - WebFlux)

#### Route Definition Pattern

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: CircuitBreaker
          args:
            name: defaultCircuitBreaker
            fallbackUri: forward:/fallback
        - name: Retry
          args:
            retries: 1
            statuses: SERVICE_UNAVAILABLE
            methods: POST
      routes:
        # Customer Service Route
        - id: customers-service
          uri: lb://customers-service        # Load-balanced URI
          predicates:
            - Path=/api/customer/**           # Route matching predicate
          filters:
            - StripPrefix=2                   # Strip /api/customer prefix
            
        # Visits Service Route
        - id: visits-service
          uri: lb://visits-service
          predicates:
            - Path=/api/visit/**
          filters:
            - StripPrefix=2
            
        # Vets Service Route
        - id: vets-service
          uri: lb://vets-service
          predicates:
            - Path=/api/vet/**
          filters:
            - StripPrefix=2
            
        # GenAI Service with Custom Circuit Breaker
        - id: genai-service
          uri: lb://genai-service
          predicates:
            - Path=/api/genai/**
          filters:
            - StripPrefix=2
            - CircuitBreaker=name=genaiCircuitBreaker,fallbackUri=/fallback
```

#### Gateway Predicates

- **Path predicates:** Match by URL path pattern
- Matches are applied in order
- First matching route handles the request

#### Gateway Filters

**Global Filters (Applied to All Routes):**
1. **CircuitBreaker Filter** - Default circuit breaker with fallback
2. **Retry Filter** - Retry with specific conditions (SERVICE_UNAVAILABLE, POST only)

**Per-Route Filters:**
- **StripPrefix** - Removes path segments before forwarding
  - `StripPrefix=2` removes `/api/customer` leaving `/owners`
  - Example: `/api/customer/owners/1` → `http://customers-service/owners/1`
- **CircuitBreaker** - Custom circuit breaker per route

#### Request Flow

```
Client Request → Gateway (8081)
    ↓
Pattern Matching (e.g., Path=/api/customer/**)
    ↓
Global Filters (CircuitBreaker, Retry)
    ↓
Per-Route Filters (StripPrefix)
    ↓
Service Discovery (Eureka lookup for customers-service)
    ↓
Load Balancing (if multiple instances)
    ↓
Backend Service (8081 → customers-service)
    ↓
Response → Client
```

### Gateway Customization

The `ApiGatewayApplication` provides:

1. **LoadBalanced WebClient** - For programmatic HTTP calls from controllers
2. **RouterFunction** - Custom route handling for static content:
   ```java
   @Bean
   RouterFunction<?> routerFunction() {
       return RouterFunctions.resources("/**", new ClassPathResource("static/"))
           .andRoute(RequestPredicates.GET("/"),
               request -> ServerResponse.ok().contentType(TEXT_HTML)
                   .bodyValue(indexHtml));  // Serve index.html for SPA
   }
   ```

3. **Circuit Breaker Customization** - 10-second timeout for all services

---

## 8. Caching Strategy

### Cache Configuration

**Vets Service** enables caching in production:

```java
@Configuration
@EnableCaching
@Profile("production")  // Cache only in production, not in tests
class CacheConfig {
}
```

**Configuration in YAML:**
```yaml
spring:
  cache:
    cache-names: vets
```

### Cache Usage

```java
@GetMapping
@Cacheable("vets")  // Cache result under "vets" cache name
public List<Vet> showResourcesVetList() {
    return vetRepository.findAll();
}
```

### Cache Backend

**Default:** Caffeine (high-performance in-memory cache)
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

---

## 9. Observability & Monitoring

### Distributed Tracing

**Framework:** Spring Cloud Sleuth + Brave + Zipkin

**Configuration in all services:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of traces (for demo)
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}
```

**Traced Operations:**
- All HTTP requests through API Gateway
- Service-to-service calls (CustomersServiceClient, VisitsServiceClient)
- Database operations
- Cache operations

**Benefits:**
- End-to-end request tracing across microservices
- Latency visualization
- Dependency graph discovery

### Metrics & Monitoring

**Framework:** Micrometer + Prometheus

#### Metric Configuration

```java
@Configuration
public class MetricConfig {
    
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "petclinic");
    }
    
    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

#### Method-Level Metrics

```java
@Timed("petclinic.owner")
public class OwnerResource { ... }

@Timed("petclinic.visit")
public class VisitResource { ... }
```

#### Prometheus Integration

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Metrics exposed at: `http://localhost:{service-port}/actuator/prometheus`

#### Prometheus Server

**Deployment:** Containerized Prometheus server (Port 9091)
- Scrapes metrics from all services' `/actuator/prometheus` endpoints
- Stores time-series metrics data
- Provides PromQL query interface
- Configuration in `docker/prometheus/`

#### Grafana Server

**Deployment:** Containerized Grafana server (Port 3030)
- Pre-configured with Prometheus as data source
- Provides visualization dashboards for:
  - Service health and availability
  - Request rates and latencies
  - JVM metrics (memory, threads, GC)
  - Database connection pool metrics
  - Circuit breaker states
- Configuration in `docker/grafana/`

### Spring Boot Actuator

All services include Actuator for health checks and monitoring:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Exposed Endpoints:**
- `/actuator/health` - Service health status
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/env` - Environment properties
- `/actuator/beans` - Bean metadata

### Spring Boot Admin

Admin Server (`admin-server`) provides centralized monitoring:
- Real-time health monitoring for all services
- Log streaming
- Application metrics dashboard
- Thread monitoring
- Memory/CPU visualization

---

## 10. GenAI Integration (Spring AI)

### Generative AI Service Architecture

**Technology:** Spring AI 1.0.0-M5 with OpenAI/Azure OpenAI

```java
@EnableDiscoveryClient
@SpringBootApplication
public class GenAIServiceApplication {
    // Registers with Eureka for service discovery
}
```

#### Reactive Web Framework

```yaml
spring:
  main:
    web-application-type: reactive  # WebFlux instead of WebMvc
```

### ChatClient Implementation

```java
@RestController
@RequestMapping("/")
public class PetclinicChatClient {
    
    private final ChatClient chatClient;
    
    public PetclinicChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
            .defaultSystem("""
                You are a friendly AI assistant for Spring PetClinic...
                """)
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory, 10),  // Keep last 10 messages
                new SimpleLoggerAdvisor()
            )
            .defaultFunctions("listOwners", "addOwnerToPetclinic", "addPetToOwner", "listVets")
            .build();
    }
    
    @PostMapping("/chatclient")
    public String exchange(@RequestBody String query) {
        try {
            return this.chatClient
                .prompt()
                .user(u -> u.text(query))
                .call()
                .content();
        } catch (Exception e) {
            LOG.error("Error processing chat message", e);
            return "Chat is currently unavailable. Please try again later.";
        }
    }
}
```

### AI Function Integration

The ChatClient can invoke functions:
- `listOwners` - Retrieve owners from Customers Service
- `addOwnerToPetclinic` - Create new owner
- `addPetToOwner` - Add pet to owner
- `listVets` - Get available veterinarians

### AI Configuration

```yaml
management:
  ai:
    chat:
      client:
        enabled: true
    # These apply when using spring-ai-openai-spring-boot-starter
    openai:
      api-key: ${OPENAI_API_KEY:demo}
      chat:
        options:
          temperature: 0.7
          model: gpt-4o-mini
    # These apply when using spring-ai-azure-openai-spring-boot-starter
    azure:
      openai:
        api-key: ${AZURE_OPENAI_KEY}
        endpoint: ${AZURE_OPENAI_ENDPOINT}
        chat:
          options:
            temperature: 0.7
            deployment-name: gpt-4o
```

### AI Data Provider

```java
// AIDataProvider component supplies context data to AI functions
// Can fetch owner/pet/vet data from respective microservices
```

---

## 11. Docker Orchestration

### Deployment Structure

**docker-compose.yml** defines the complete stack:

```yaml
services:
  config-server:
    image: springcommunity/spring-petclinic-config-server
    ports:
      - 8888:8888
    healthcheck: curl -I http://config-server:8888
    
  discovery-server:
    image: springcommunity/spring-petclinic-discovery-server
    depends_on:
      config-server: service_healthy
    ports:
      - 8761:8761
      
  customers-service:
    image: springcommunity/spring-petclinic-customers-service
    environment:
      SPRING_PROFILES_ACTIVE: docker,postgresql
      ZIPKIN_URL: http://tracing-server:9411/api/v2/spans
    depends_on:
      config-server: service_healthy
      discovery-server: service_healthy
      postgres: service_started
      
  # Similar for visits-service, vets-service, api-gateway...

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: petclinic
      POSTGRES_USER: petclinic
      POSTGRES_PASSWORD: petclinic
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgresql/init.sql:/docker-entrypoint-initdb.d/init.sql
    deploy:
      resources:
        limits:
          memory: 256M

  tracing-server:
    image: openzipkin/zipkin
    ports:
      - 9411:9411
    deploy:
      resources:
        limits:
          memory: 512M

  admin-server:
    image: springcommunity/spring-petclinic-admin-server
    depends_on:
      config-server: service_healthy
      discovery-server: service_healthy
    ports:
      - 9090:9090
    deploy:
      resources:
        limits:
          memory: 512M

  grafana-server:
    build: ./docker/grafana
    ports:
      - 3030:3030
    deploy:
      resources:
        limits:
          memory: 256M

  prometheus-server:
    build: ./docker/prometheus
    ports:
      - 9091:9090
    deploy:
      resources:
        limits:
          memory: 256M

volumes:
  postgres_data:
```

### Service Dependencies

**Startup Order:**
1. Config Server (all services depend on it)
2. Discovery Server (depends on Config Server)
3. PostgreSQL (database)
4. Domain Services (depend on Config + Discovery + Postgres)
5. API Gateway (depends on Discovery)

### Environment Configuration

Services use environment variables for Docker profiles:
```bash
SPRING_PROFILES_ACTIVE=docker,postgresql
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/petclinic
ZIPKIN_URL=http://tracing-server:9411/api/v2/spans
CONFIG_SERVER_URL=http://config-server:8888/
```

---

## 12. Key Architectural Patterns

### 1. Service-to-Service Communication

**Pattern:** Choreography-based with fallbacks
- Gateway orchestrates complex operations
- Services call each other via REST with circuit breakers
- Fallback to empty data if service unavailable

### 2. Data Consistency

**Pattern:** Eventual Consistency
- Each service owns its database
- No distributed transactions
- API Gateway composes data from multiple services
- Accepts stale data (vets are cached)

### 3. Configuration Management

**Pattern:** Externalized Configuration Server
- Centralized config in Git repository
- Services pull config at startup
- Environment-specific profiles (docker, postgresql, production)
- Override via environment variables

### 4. Resilience

**Patterns:**
- **Circuit Breaker:** Prevents cascading failures (Resilience4j)
- **Retry:** Automatic retry on transient failures
- **Timeout:** 10-second timeout on service calls
- **Fallback:** Returns safe defaults when services fail
- **Caching:** Reduces load on backend services

### 5. Observability

**Patterns:**
- **Distributed Tracing:** Zipkin with Brave instrumentation
- **Metrics:** Prometheus via Micrometer
- **Health Checks:** Docker healthchecks + Actuator
- **Centralized Monitoring:** Spring Boot Admin

### 6. Testing Strategy

**Patterns:**
- **Unit Tests:** WebMvcTest for controllers with mocked dependencies
- **Integration Tests:** WebFluxTest for reactive components
- **Circuit Breaker Tests:** Verify fallback behavior
- **Profile-Based Testing:** Use "test" profile to disable caching

---

## 13. Build & Deployment

### Maven Build

**Prerequisites:** Java 25 or higher is required

The project uses `maven-enforcer-plugin` to verify Java version at build time:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <configuration>
        <rules>
            <requireJavaVersion>
                <version>[${java.version},)</version>
            </requireJavaVersion>
        </rules>
    </configuration>
</plugin>
```

```bash
# Build all services
mvn clean package

# Build specific service
mvn -pl spring-petclinic-customers-service clean package

# Skip tests
mvn clean package -DskipTests

# Build Docker images (requires -PbuildDocker profile)
mvn clean package -PbuildDocker
```

### Docker Image Building

**Base Image:** Eclipse Temurin Java 25

The Dockerfile uses a multi-stage build:
```dockerfile
FROM eclipse-temurin:25 AS builder
WORKDIR application
ARG ARTIFACT_NAME
COPY ${ARTIFACT_NAME}.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:25
WORKDIR application
# Layered jar approach for optimal caching
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

Configured via Maven plugin with platform support:
```xml
<properties>
    <java.version>25</java.version>
    <docker.image.prefix>springcommunity</docker.image.prefix>
    <container.platform>linux/amd64</container.platform>
    <!-- For Apple Silicon: linux/arm64 -->
    <container.executable>docker</container.executable>
    <!-- podman is also supported -->
</properties>
```

### Development Workflow

```bash
# Start full stack
docker-compose up

# Access services
- API Gateway: http://localhost:8080
- Customers Service: http://localhost:8081
- Visits Service: http://localhost:8082
- Vets Service: http://localhost:8083
- GenAI Service: http://localhost:8084
- Discovery (Eureka): http://localhost:8761
- Config Server: http://localhost:8888
- Admin Server: http://localhost:9090
- Zipkin Tracing: http://localhost:9411
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3030
```

---

## 14. Notable Implementation Details

### Spring Boot 3.5 Specifics

1. **Jakarta EE Migration:** Uses `jakarta.persistence.*` instead of `javax.persistence.*`
2. **Native Compilation Ready:** GraalVM native image support
3. **Method Validation:** Uses Jakarta validation annotations
4. **Error Attributes:** Enhanced error responses with method validation errors

### Reactive Programming

- **API Gateway:** Uses Spring WebFlux (reactive)
- **Service Clients:** Reactive WebClient with Mono/Flux
- **GenAI Service:** Reactive web-application-type
- **Domain Services:** Traditional WebMvc (simplicity)

### REST API Design

- **DTOs:** Separate DTOs for requests/responses vs entities
- **Mappers:** Custom mappers for entity ↔ DTO conversion
- **Validation:** Jakarta validation annotations on DTOs
- **Error Handling:** GlobalExceptionHandler for consistent error responses

### Database Design

- **Shared Schema:** All services use same PostgreSQL database (simplified for demo)
- **Declarative Relationships:** JPA OneToMany/ManyToOne with cascade settings
- **Lazy Loading:** Strategic use of EAGER loading to avoid N+1 queries
- **SQL Initialization:** Schema and data loaded from classpath scripts

---

## 15. Future Enhancements (Patterns for Extension)

1. **Event-Driven Communication:** Add Kafka/RabbitMQ for async events
2. **API Versioning:** Implement v1/, v2/ versioning in gateway routes
3. **Authentication:** Add OAuth2/JWT with Spring Security
4. **Rate Limiting:** Implement token bucket rate limiter in gateway
5. **Saga Pattern:** For distributed transactions across services
6. **CQRS:** Separate read/write models for complex queries
7. **Kubernetes:** Migrate from docker-compose to K8s with Helm charts
8. **Service Mesh:** Add Istio for advanced networking patterns

---

## Summary

This Spring PetClinic microservices implementation showcases:
- Modern Spring Cloud patterns (discovery, config, gateway, circuit breakers)
- Reactive programming with Spring WebFlux
- Comprehensive observability (tracing, metrics, monitoring)
- Production-ready resilience patterns
- Clean separation of concerns across services
- AI/LLM integration for domain-specific tasks
- Docker containerization with orchestration
- Extensive test coverage with multiple testing strategies

The architecture balances sophistication with simplicity, making it an excellent reference implementation for learning cloud-native Spring Boot development.
