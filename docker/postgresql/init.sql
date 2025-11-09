-- Initialize PostgreSQL database for Spring PetClinic Microservices
-- This script creates the necessary schemas for each service

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS customers;
CREATE SCHEMA IF NOT EXISTS visits;
CREATE SCHEMA IF NOT EXISTS vets;

-- Grant permissions to the petclinic user
GRANT ALL PRIVILEGES ON SCHEMA customers TO petclinic;
GRANT ALL PRIVILEGES ON SCHEMA visits TO petclinic;
GRANT ALL PRIVILEGES ON SCHEMA vets TO petclinic;

-- Set default schema search path
ALTER USER petclinic SET search_path TO customers, visits, vets, public;