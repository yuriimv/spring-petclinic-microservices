# Spring Boot 3.5 PostgreSQL Integration Features

## Automatic Application Name Configuration

With Spring Boot 3.5, PostgreSQL connections automatically use the `spring.application.name` property to set the `application_name` parameter in PostgreSQL connections. This provides better visibility and monitoring capabilities.

### How It Works

When a Spring Boot 3.5 application connects to PostgreSQL, it automatically sets the `application_name` connection parameter using the value from `spring.application.name`. This happens without any additional configuration.

### Current Configuration

All services in the Spring PetClinic Microservices application have their `spring.application.name` properly configured:

- **config-server**: `spring.application.name: config-server`
- **discovery-server**: `spring.application.name: discovery-server`
- **api-gateway**: `spring.application.name: api-gateway`
- **customers-service**: `spring.application.name: customers-service`
- **visits-service**: `spring.application.name: visits-service`
- **vets-service**: `spring.application.name: vets-service`
- **genai-service**: `spring.application.name: genai-service`
- **admin-server**: `spring.application.name: admin-server`

### Benefits

1. **Database Monitoring**: Each service's database connections are clearly identifiable in PostgreSQL monitoring tools
2. **Connection Tracking**: Easy to track which service is making specific database queries
3. **Performance Analysis**: Analyze database performance per service
4. **Troubleshooting**: Quickly identify which service is causing database issues

### Monitoring Database Connections

You can monitor active connections and see which service is making queries:

```sql
-- View active connections by application
SELECT 
    application_name,
    client_addr,
    state,
    query_start,
    LEFT(query, 100) as query_preview
FROM pg_stat_activity 
WHERE application_name IS NOT NULL
ORDER BY application_name, query_start DESC;

-- Count connections per service
SELECT 
    application_name,
    COUNT(*) as connection_count,
    COUNT(CASE WHEN state = 'active' THEN 1 END) as active_connections
FROM pg_stat_activity 
WHERE application_name IS NOT NULL
GROUP BY application_name
ORDER BY connection_count DESC;
```

### Docker Compose Configuration

The Docker Compose configuration includes:

- PostgreSQL 15 service with persistent storage
- Proper environment variables for database connection
- Service dependencies ensuring PostgreSQL starts before dependent services
- PostgreSQL profiles activated for database services (`docker,postgresql`)

### Environment Variables

Each database service is configured with:

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker,postgresql
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/petclinic
  - SPRING_DATASOURCE_USERNAME=petclinic
  - SPRING_DATASOURCE_PASSWORD=petclinic
```

### Starting the Services

To start the services with PostgreSQL:

```bash
# Start PostgreSQL first (optional, dependencies handle this)
docker-compose up -d postgres

# Start all services
docker-compose up

# Or start in detached mode
docker-compose up -d
```

The automatic application_name feature will work immediately without any additional configuration changes.