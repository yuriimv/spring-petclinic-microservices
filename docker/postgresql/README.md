# PostgreSQL Configuration for Spring PetClinic Microservices

## Overview

This directory contains PostgreSQL configuration files for the Spring PetClinic Microservices application.

## Features

### Automatic Application Name Configuration

With Spring Boot 3.5+, PostgreSQL connections automatically use the `spring.application.name` property to set the `application_name` in PostgreSQL. This provides better visibility in database monitoring and logging.

Each service will appear in PostgreSQL with its respective application name:
- `customers-service`
- `visits-service` 
- `vets-service`
- `api-gateway`
- `discovery-server`
- `config-server`
- `admin-server`
- `genai-service`

## Database Schema

The PostgreSQL setup includes:
- **customers schema**: owners, pets, and pet types
- **visits schema**: visit records
- **vets schema**: veterinarians and specialties

## Usage

### Starting with Docker Compose

```bash
# Start PostgreSQL first
docker-compose up -d postgres

# Start all services
docker-compose up
```

### Manual PostgreSQL Setup

If running services locally against a PostgreSQL instance:

1. Create database: `petclinic`
2. Create user: `petclinic` with password `petclinic`
3. Run the initialization script: `docker/postgresql/init.sql`
4. Set the `postgresql` profile when starting services

### Environment Variables

The following environment variables are configured in docker-compose.yml:

- `SPRING_PROFILES_ACTIVE=docker,postgresql`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/petclinic`
- `SPRING_DATASOURCE_USERNAME=petclinic`
- `SPRING_DATASOURCE_PASSWORD=petclinic`

## Monitoring

You can monitor database connections and see which service is making queries by checking the `application_name` column in PostgreSQL system views:

```sql
SELECT application_name, client_addr, state, query 
FROM pg_stat_activity 
WHERE application_name LIKE '%service%';
```