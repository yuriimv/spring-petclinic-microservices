#!/bin/bash

# Spring PetClinic Performance Benchmark Script
# Runs performance tests and compares metrics with baseline

set -e

echo "=== Spring PetClinic Performance Benchmark ==="
echo "Running performance tests after Spring Boot 3.5.5 migration"
echo

# Configuration
API_GATEWAY_URL="http://localhost:8080"
PROMETHEUS_URL="http://localhost:9091"
JMETER_TEST_PLAN="spring-petclinic-api-gateway/src/test/jmeter/petclinic_test_plan.jmx"
RESULTS_DIR="performance_results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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
        "HEADER")
            echo -e "${BLUE}=== $message ===${NC}"
            ;;
    esac
}

# Function to check if service is responding
check_service() {
    local url=$1
    local service_name=$2
    
    if curl -s --max-time 10 "$url" > /dev/null 2>&1; then
        print_status "SUCCESS" "$service_name is responding"
        return 0
    else
        print_status "FAIL" "$service_name is not responding"
        return 1
    fi
}

# Function to get current metrics from Prometheus
get_prometheus_metrics() {
    local metric_name=$1
    local query_params=${2:-""}
    
    local response=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=$metric_name$query_params")
    echo "$response"
}

# Function to extract metric value from Prometheus response
extract_metric_value() {
    local response=$1
    echo "$response" | grep -o '"value":\[[^]]*\]' | grep -o '[0-9.]*' | tail -1
}

# Function to run basic load test using curl
run_curl_load_test() {
    local endpoint=$1
    local requests=$2
    local concurrent=${3:-5}
    
    print_status "INFO" "Running load test: $requests requests to $endpoint with $concurrent concurrent connections"
    
    # Create temporary script for parallel requests
    local temp_script="/tmp/load_test_$$.sh"
    cat > "$temp_script" << EOF
#!/bin/bash
for i in \$(seq 1 $((requests / concurrent))); do
    curl -s "$endpoint" > /dev/null 2>&1 &
done
wait
EOF
    
    chmod +x "$temp_script"
    
    # Run the load test and measure time
    local start_time=$(date +%s.%N)
    
    for i in $(seq 1 $concurrent); do
        "$temp_script" &
    done
    wait
    
    local end_time=$(date +%s.%N)
    local duration=$(echo "$end_time - $start_time" | bc -l)
    local rps=$(echo "scale=2; $requests / $duration" | bc -l)
    
    rm -f "$temp_script"
    
    echo "$duration,$rps"
}

# Function to collect baseline metrics
collect_baseline_metrics() {
    print_status "INFO" "Collecting baseline metrics..."
    
    # JVM Memory metrics
    local jvm_memory=$(get_prometheus_metrics "jvm_memory_used_bytes")
    local jvm_memory_value=$(extract_metric_value "$jvm_memory")
    
    # HTTP request metrics
    local http_requests=$(get_prometheus_metrics "http_server_requests_seconds_count")
    local http_requests_value=$(extract_metric_value "$http_requests")
    
    # System CPU usage
    local cpu_usage=$(get_prometheus_metrics "system_cpu_usage")
    local cpu_usage_value=$(extract_metric_value "$cpu_usage")
    
    echo "baseline_jvm_memory_bytes,$jvm_memory_value"
    echo "baseline_http_requests_count,$http_requests_value"
    echo "baseline_cpu_usage,$cpu_usage_value"
}

# Function to run performance tests
run_performance_tests() {
    print_status "HEADER" "Running Performance Tests"
    
    # Create results directory
    mkdir -p "$RESULTS_DIR"
    local results_file="$RESULTS_DIR/benchmark_$TIMESTAMP.csv"
    
    # Write CSV header
    echo "test_name,endpoint,requests,duration_seconds,requests_per_second,avg_response_time_ms" > "$results_file"
    
    # Test endpoints
    local endpoints=(
        "/api/customer/owners"
        "/api/vet/vets" 
        "/api/visit/owners/1/pets/1/visits"
        "/"
    )
    
    local request_counts=(50 100 200)
    
    for endpoint in "${endpoints[@]}"; do
        for count in "${request_counts[@]}"; do
            print_status "INFO" "Testing $endpoint with $count requests..."
            
            local full_url="$API_GATEWAY_URL$endpoint"
            local result=$(run_curl_load_test "$full_url" "$count" 10)
            local duration=$(echo "$result" | cut -d, -f1)
            local rps=$(echo "$result" | cut -d, -f2)
            local avg_response_time=$(echo "scale=2; 1000 / $rps" | bc -l)
            
            # Log results
            echo "load_test_${count},$endpoint,$count,$duration,$rps,$avg_response_time" >> "$results_file"
            
            print_status "SUCCESS" "Completed: $rps req/sec, avg response time: ${avg_response_time}ms"
            
            # Wait between tests
            sleep 2
        done
    done
    
    print_status "SUCCESS" "Performance test results saved to $results_file"
}

# Function to collect post-test metrics
collect_post_test_metrics() {
    print_status "INFO" "Collecting post-test metrics..."
    
    local results_file="$RESULTS_DIR/metrics_$TIMESTAMP.csv"
    echo "metric_name,value" > "$results_file"
    
    # Wait for metrics to be updated
    sleep 10
    
    # JVM Memory metrics
    local jvm_memory=$(get_prometheus_metrics "jvm_memory_used_bytes")
    local jvm_memory_value=$(extract_metric_value "$jvm_memory")
    echo "post_test_jvm_memory_bytes,$jvm_memory_value" >> "$results_file"
    
    # HTTP request metrics
    local http_requests=$(get_prometheus_metrics "http_server_requests_seconds_count")
    local http_requests_value=$(extract_metric_value "$http_requests")
    echo "post_test_http_requests_count,$http_requests_value" >> "$results_file"
    
    # System CPU usage
    local cpu_usage=$(get_prometheus_metrics "system_cpu_usage")
    local cpu_usage_value=$(extract_metric_value "$cpu_usage")
    echo "post_test_cpu_usage,$cpu_usage_value" >> "$results_file"
    
    # Response time percentiles
    local p95_response_time=$(get_prometheus_metrics "http_server_requests_seconds" "{quantile=\"0.95\"}")
    local p95_value=$(extract_metric_value "$p95_response_time")
    echo "p95_response_time_seconds,$p95_value" >> "$results_file"
    
    local p99_response_time=$(get_prometheus_metrics "http_server_requests_seconds" "{quantile=\"0.99\"}")
    local p99_value=$(extract_metric_value "$p99_response_time")
    echo "p99_response_time_seconds,$p99_value" >> "$results_file"
    
    print_status "SUCCESS" "Metrics saved to $results_file"
}

# Function to run JMeter test if available
run_jmeter_test() {
    if command -v jmeter >/dev/null 2>&1 && [ -f "$JMETER_TEST_PLAN" ]; then
        print_status "INFO" "Running JMeter performance test..."
        
        local jmeter_results="$RESULTS_DIR/jmeter_results_$TIMESTAMP.jtl"
        local jmeter_report="$RESULTS_DIR/jmeter_report_$TIMESTAMP"
        
        # Run JMeter test
        jmeter -n -t "$JMETER_TEST_PLAN" -l "$jmeter_results" \
               -JPETCLINC_HOST=localhost -JPETCLINIC_PORT=8080 \
               -e -o "$jmeter_report" 2>/dev/null || true
        
        if [ -f "$jmeter_results" ]; then
            print_status "SUCCESS" "JMeter test completed. Results in $jmeter_results"
            print_status "INFO" "JMeter HTML report generated in $jmeter_report"
        else
            print_status "FAIL" "JMeter test failed"
        fi
    else
        print_status "INFO" "JMeter not available or test plan not found. Skipping JMeter test."
    fi
}

# Function to analyze results
analyze_results() {
    print_status "HEADER" "Performance Analysis"
    
    local latest_results=$(ls -t "$RESULTS_DIR"/benchmark_*.csv 2>/dev/null | head -1)
    local latest_metrics=$(ls -t "$RESULTS_DIR"/metrics_*.csv 2>/dev/null | head -1)
    
    if [ -f "$latest_results" ]; then
        print_status "INFO" "Performance Test Summary:"
        echo
        
        # Calculate average RPS for each endpoint
        while IFS=, read -r test_name endpoint requests duration rps avg_response_time; do
            if [ "$test_name" != "test_name" ]; then
                printf "%-30s %-10s req/sec, %-8s ms avg response\n" "$endpoint" "$rps" "$avg_response_time"
            fi
        done < "$latest_results"
        
        echo
        
        # Find best and worst performing endpoints
        local best_rps=$(tail -n +2 "$latest_results" | cut -d, -f5 | sort -nr | head -1)
        local worst_rps=$(tail -n +2 "$latest_results" | cut -d, -f5 | sort -n | head -1)
        
        print_status "SUCCESS" "Best performance: $best_rps req/sec"
        print_status "INFO" "Lowest performance: $worst_rps req/sec"
    fi
    
    if [ -f "$latest_metrics" ]; then
        print_status "INFO" "System Metrics Summary:"
        echo
        
        while IFS=, read -r metric_name value; do
            if [ "$metric_name" != "metric_name" ]; then
                case "$metric_name" in
                    *memory*)
                        local memory_mb=$(echo "scale=2; $value / 1024 / 1024" | bc -l 2>/dev/null || echo "N/A")
                        printf "%-30s %s MB\n" "$metric_name" "$memory_mb"
                        ;;
                    *cpu*)
                        local cpu_percent=$(echo "scale=2; $value * 100" | bc -l 2>/dev/null || echo "N/A")
                        printf "%-30s %s%%\n" "$metric_name" "$cpu_percent"
                        ;;
                    *response_time*)
                        local response_ms=$(echo "scale=2; $value * 1000" | bc -l 2>/dev/null || echo "N/A")
                        printf "%-30s %s ms\n" "$metric_name" "$response_ms"
                        ;;
                    *)
                        printf "%-30s %s\n" "$metric_name" "$value"
                        ;;
                esac
            fi
        done < "$latest_metrics"
    fi
}

# Main execution
main() {
    print_status "HEADER" "Pre-flight Checks"
    
    # Check if services are running
    if ! check_service "$API_GATEWAY_URL/actuator/health" "API Gateway"; then
        print_status "FAIL" "API Gateway is not running. Please start the services first."
        exit 1
    fi
    
    if ! check_service "$PROMETHEUS_URL" "Prometheus"; then
        print_status "FAIL" "Prometheus is not running. Metrics collection will be limited."
    fi
    
    # Install bc if not available (for calculations)
    if ! command -v bc >/dev/null 2>&1; then
        print_status "INFO" "Installing bc for calculations..."
        if command -v brew >/dev/null 2>&1; then
            brew install bc >/dev/null 2>&1 || true
        fi
    fi
    
    # Collect baseline metrics
    collect_baseline_metrics > "$RESULTS_DIR/baseline_metrics_$TIMESTAMP.csv" 2>/dev/null || true
    
    # Run performance tests
    run_performance_tests
    
    # Run JMeter test if available
    run_jmeter_test
    
    # Collect post-test metrics
    collect_post_test_metrics
    
    # Analyze results
    analyze_results
    
    print_status "SUCCESS" "Performance benchmark completed!"
    print_status "INFO" "Results are available in the $RESULTS_DIR directory"
}

# Run main function
main "$@"