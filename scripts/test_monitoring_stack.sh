#!/bin/bash

# Spring PetClinic Monitoring Stack Integration Test
# Tests Prometheus metrics collection, Grafana dashboard functionality, and Zipkin distributed tracing

set -e

echo "=== Spring PetClinic Monitoring Stack Integration Test ==="
echo "Testing Prometheus, Grafana, and Zipkin integration after Spring Boot 3.5.5 migration"
echo

# Configuration
API_GATEWAY_URL="http://localhost:8080"
PROMETHEUS_URL="http://localhost:9091"
GRAFANA_URL="http://localhost:3030"
ZIPKIN_URL="http://localhost:9411"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS")
            echo -e "${GREEN}✓ $message${NC}"
            ;;
        "FAIL")
            echo -e "${RED}✗ $message${NC}"
            ;;
        "INFO")
            echo -e "${YELLOW}ℹ $message${NC}"
            ;;
    esac
}

# Function to check if service is responding
check_service() {
    local url=$1
    local service_name=$2
    local timeout=${3:-10}
    
    if curl -s --max-time $timeout "$url" > /dev/null 2>&1; then
        print_status "SUCCESS" "$service_name is responding at $url"
        return 0
    else
        print_status "FAIL" "$service_name is not responding at $url"
        return 1
    fi
}

# Function to check if JSON response contains expected data
check_json_response() {
    local url=$1
    local expected_key=$2
    local service_name=$3
    
    local response=$(curl -s "$url" 2>/dev/null)
    if echo "$response" | grep -q "$expected_key"; then
        print_status "SUCCESS" "$service_name returned expected data"
        return 0
    else
        print_status "FAIL" "$service_name did not return expected data"
        echo "Response: $response"
        return 1
    fi
}

# Function to generate some traffic for tracing
generate_traffic() {
    print_status "INFO" "Generating traffic to create traces and metrics..."
    
    # Make requests to different services through API Gateway
    curl -s "$API_GATEWAY_URL/api/customer/owners" > /dev/null 2>&1 || true
    curl -s "$API_GATEWAY_URL/api/vet/vets" > /dev/null 2>&1 || true
    curl -s "$API_GATEWAY_URL/api/visit/owners/1/pets/1/visits" > /dev/null 2>&1 || true
    
    # Wait a bit for metrics to be collected
    sleep 5
}

echo "1. Testing Prometheus Metrics Collection"
echo "========================================"

# Check if Prometheus server is running
if check_service "$PROMETHEUS_URL" "Prometheus Server"; then
    
    # Check Prometheus targets
    print_status "INFO" "Checking Prometheus targets..."
    if check_json_response "$PROMETHEUS_URL/api/v1/targets" "api-gateway" "Prometheus targets"; then
        
        # Generate some traffic first
        generate_traffic
        
        # Check if metrics are being collected from services
        print_status "INFO" "Checking if metrics are being collected from services..."
        
        # Check for Spring Boot actuator metrics
        if curl -s "$PROMETHEUS_URL/api/v1/query?query=up" | grep -q '"result"'; then
            print_status "SUCCESS" "Prometheus is collecting 'up' metrics from services"
        else
            print_status "FAIL" "Prometheus is not collecting basic metrics"
        fi
        
        # Check for JVM metrics (common Spring Boot metric)
        if curl -s "$PROMETHEUS_URL/api/v1/query?query=jvm_memory_used_bytes" | grep -q '"result"'; then
            print_status "SUCCESS" "Prometheus is collecting JVM memory metrics"
        else
            print_status "FAIL" "Prometheus is not collecting JVM metrics"
        fi
        
        # Check for HTTP request metrics
        if curl -s "$PROMETHEUS_URL/api/v1/query?query=http_server_requests_seconds_count" | grep -q '"result"'; then
            print_status "SUCCESS" "Prometheus is collecting HTTP request metrics"
        else
            print_status "FAIL" "Prometheus is not collecting HTTP request metrics"
        fi
        
    fi
else
    print_status "FAIL" "Cannot test Prometheus metrics - service not available"
fi

echo
echo "2. Testing Grafana Dashboard Functionality"
echo "=========================================="

# Check if Grafana server is running
if check_service "$GRAFANA_URL" "Grafana Server"; then
    
    # Check Grafana API
    print_status "INFO" "Testing Grafana API..."
    if check_json_response "$GRAFANA_URL/api/health" "ok" "Grafana health check"; then
        
        # Check if Prometheus datasource is configured
        print_status "INFO" "Checking Prometheus datasource configuration..."
        if curl -s "$GRAFANA_URL/api/datasources" | grep -q "Prometheus"; then
            print_status "SUCCESS" "Prometheus datasource is configured in Grafana"
        else
            print_status "FAIL" "Prometheus datasource is not configured in Grafana"
        fi
        
        # Check if dashboards are available
        print_status "INFO" "Checking available dashboards..."
        dashboard_count=$(curl -s "$GRAFANA_URL/api/search" | grep -o '"title"' | wc -l)
        if [ "$dashboard_count" -gt 0 ]; then
            print_status "SUCCESS" "Found $dashboard_count dashboard(s) in Grafana"
        else
            print_status "FAIL" "No dashboards found in Grafana"
        fi
        
    fi
else
    print_status "FAIL" "Cannot test Grafana functionality - service not available"
fi

echo
echo "3. Testing Zipkin Distributed Tracing"
echo "====================================="

# Check if Zipkin server is running
if check_service "$ZIPKIN_URL" "Zipkin Server"; then
    
    # Generate traffic to create traces
    print_status "INFO" "Generating traffic to create distributed traces..."
    generate_traffic
    
    # Wait for traces to be processed
    sleep 10
    
    # Check if traces are being collected
    print_status "INFO" "Checking for collected traces..."
    traces_response=$(curl -s "$ZIPKIN_URL/api/v2/traces?limit=10")
    
    if echo "$traces_response" | grep -q '\['; then
        trace_count=$(echo "$traces_response" | grep -o '\[\[' | wc -l)
        if [ "$trace_count" -gt 0 ]; then
            print_status "SUCCESS" "Found $trace_count trace(s) in Zipkin"
            
            # Check for service names in traces
            if echo "$traces_response" | grep -q "api-gateway\|customers-service\|vets-service\|visits-service"; then
                print_status "SUCCESS" "Traces contain expected service names"
            else
                print_status "FAIL" "Traces do not contain expected service names"
            fi
        else
            print_status "FAIL" "No traces found in Zipkin"
        fi
    else
        print_status "FAIL" "Invalid response from Zipkin traces API"
    fi
    
    # Check Zipkin services endpoint
    print_status "INFO" "Checking discovered services in Zipkin..."
    services_response=$(curl -s "$ZIPKIN_URL/api/v2/services")
    if echo "$services_response" | grep -q "api-gateway"; then
        print_status "SUCCESS" "Zipkin has discovered microservices"
    else
        print_status "FAIL" "Zipkin has not discovered expected microservices"
    fi
    
else
    print_status "FAIL" "Cannot test Zipkin tracing - service not available"
fi

echo
echo "4. Testing Service Actuator Endpoints"
echo "====================================="

# Test actuator endpoints on individual services
services=("customers-service:8081" "vets-service:8083" "visits-service:8082" "api-gateway:8080")

for service in "${services[@]}"; do
    service_name=$(echo $service | cut -d: -f1)
    service_port=$(echo $service | cut -d: -f2)
    service_url="http://localhost:$service_port"
    
    print_status "INFO" "Testing $service_name actuator endpoints..."
    
    # Check health endpoint
    if check_service "$service_url/actuator/health" "$service_name health endpoint"; then
        
        # Check Prometheus metrics endpoint
        if curl -s "$service_url/actuator/prometheus" | grep -q "jvm_memory_used_bytes"; then
            print_status "SUCCESS" "$service_name Prometheus metrics endpoint is working"
        else
            print_status "FAIL" "$service_name Prometheus metrics endpoint is not working"
        fi
        
        # Check info endpoint
        if check_service "$service_url/actuator/info" "$service_name info endpoint"; then
            print_status "SUCCESS" "$service_name info endpoint is accessible"
        fi
        
    fi
done

echo
echo "=== Monitoring Stack Integration Test Complete ==="
echo
print_status "INFO" "Test completed. Check the results above for any failures."
print_status "INFO" "If all tests passed, the monitoring stack is properly integrated with Spring Boot 3.5.5"