# Design Document

## Overview

This design document outlines the approach for migrating the Spring PetClinic Microservices application from Spring Boot 3.4.1 to Spring Boot 3.5.5. The migration will be performed systematically across all modules while ensuring compatibility and leveraging new features introduced in Spring Boot 3.5.

## Architecture

The migration maintains the existing microservices architecture with the following components:
- 8 Spring Boot microservices (API Gateway, Config Server, Discovery Server, Customers, Vets, Visits, GenAI, Admin Server)
- Docker Compose orchestration
- Spring Cloud service discovery and configuration
- Monitoring stack (Prometheus, Grafana, Zipkin)

### Migration Strategy

The migration follows a **bottom-up approach**:
1. Update parent POM with new Spring Boot version
2. Update Spring Cloud BOM to compatible version
3. Update individual service configurations
4. Address deprecated properties and breaking changes
5. Update Spring AI dependencies for GenAI service
6. Validate and test the complete system

## Components and Interfaces

### 1. Parent POM Updates

**Component:** Root `pom.xml`
- **Current Version:** Spring Boot 3.4.1, Spring Cloud 2024.0.0
- **Target Version:** Spring Boot 3.5.5, Spring Cloud 2024.0.1 (or latest compatible)
- **Changes Required:**
  - Update `spring-boot-starter-parent` version
  - Update `spring-cloud.version` property
  - Verify dependency compatibility

### 2. Configuration Property Updates

**Component:** Application configuration files
- **GraphQL Properties:** Update transport-specific properties
  - `spring.graphql.path` → `spring.graphql.http.path`
  - `spring.graphql.sse.timeout` → `spring.graphql.http.sse.timeout`
- **Management Properties:** Update access log prefix format
  - `management.server.accesslog.prefix` → `management.server.{server}.accesslog.prefix`
- **Tomcat Properties:** Explicitly configure APR usage if needed
  - `server.tomcat.use-apr` (default changed to 'never')

### 3. Spring AI Integration Updates

**Component:** GenAI Service (`spring-petclinic-genai-service`)
- **Current Version:** Spring AI 1.0.0-M4
- **Target Version:** Latest stable version compatible with Spring Boot 3.5.5
- **Changes Required:**
  - Update Spring AI BOM version
  - Verify OpenAI/Azure OpenAI starter compatibility
  - Test chatbot functionality

### 4. Zipkin Configuration Updates

**Component:** Tracing configuration
- **Change:** Default sender updated to `ZipkinHttpClientSender`
- **Fallback:** `URLConnectionSender` when `HttpClient` unavailable
- **Impact:** Automatic improvement, no configuration changes needed

### 5. PostgreSQL Docker Integration

**Component:** Docker Compose configuration
- **Enhancement:** Automatic `application_name` configuration
- **Implementation:** Uses `spring.application.name` property automatically
- **Benefit:** Better database connection identification

## Data Models

No changes to existing data models are required. The migration maintains:
- Customer/Owner entities
- Pet entities  
- Veterinarian entities
- Visit entities
- All existing JPA relationships and constraints

## Error Handling

### Enhanced Error Handling (Spring Boot 3.5 Feature)

**MethodValidationResult Integration:**
- Errors from `MethodValidationResult` now included in `ErrorAttributes`
- Automatic JSON serialization safety
- Improved validation error reporting

**Implementation Strategy:**
- Leverage automatic integration (no code changes required)
- Update error handling tests to verify new error format
- Ensure frontend can handle enhanced error responses

### Migration Error Handling

**Potential Issues and Mitigation:**
1. **Dependency Conflicts:** Use Maven dependency tree analysis
2. **Configuration Deprecation:** Systematic property review and update
3. **Spring AI Compatibility:** Incremental testing with AI features
4. **Service Communication:** Gradual rollout with health checks

## Testing Strategy

### 1. Unit Testing
- **Scope:** Individual service functionality
- **Approach:** Run existing test suites with new versions
- **Validation:** Ensure all tests pass without modification

### 2. Integration Testing
- **Scope:** Service-to-service communication
- **Approach:** Test API Gateway routing to all backend services
- **Validation:** Verify Eureka registration and discovery

### 3. End-to-End Testing
- **Scope:** Complete application workflow
- **Approach:** Test user journeys through web interface
- **Validation:** Verify chatbot, CRUD operations, monitoring

### 4. Performance Testing
- **Scope:** System performance comparison
- **Approach:** Load testing with JMeter scripts
- **Validation:** Ensure no performance regression

### 5. Docker Integration Testing
- **Scope:** Containerized deployment
- **Approach:** Full docker-compose stack testing
- **Validation:** All services start and communicate properly

## Implementation Phases

### Phase 1: Core Framework Update
1. Update parent POM versions
2. Update Spring Cloud BOM
3. Build and resolve dependency conflicts

### Phase 2: Configuration Migration
1. Update deprecated configuration properties
2. Address breaking changes in property structure
3. Update Docker Compose if needed

### Phase 3: Service-Specific Updates
1. Update GenAI service Spring AI dependencies
2. Verify service-specific configurations
3. Update any custom auto-configurations

### Phase 4: Testing and Validation
1. Run comprehensive test suite
2. Perform integration testing
3. Validate Docker deployment
4. Performance benchmarking

### Phase 5: Documentation and Cleanup
1. Update README with new version information
2. Update Docker image tags
3. Clean up any temporary migration artifacts

## Risk Assessment

### High Risk
- **Spring AI Compatibility:** GenAI service depends on Spring AI framework
- **Mitigation:** Incremental testing, fallback to previous AI version if needed

### Medium Risk
- **Configuration Breaking Changes:** Property structure changes
- **Mitigation:** Systematic review of all configuration files

### Low Risk
- **Dependency Updates:** Most dependencies are backward compatible
- **Mitigation:** Standard dependency management practices

## Rollback Strategy

1. **Git Branch Strategy:** Maintain migration in feature branch
2. **Version Pinning:** Keep previous versions documented
3. **Docker Images:** Maintain previous image versions
4. **Configuration Backup:** Preserve original configuration files

## Success Criteria

1. All services start successfully with Spring Boot 3.5.5
2. No deprecation warnings in application logs
3. All existing functionality works as before
4. Performance metrics remain stable or improve
5. Docker deployment works without issues
6. Monitoring and observability features continue to function