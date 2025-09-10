#!/bin/bash

# Performance Validation Script
# Validates that Spring Boot 3.5.5 migration has no performance regressions

set -e

echo "=== Performance Validation for Spring Boot 3.5.5 Migration ==="
echo

# Configuration
RESULTS_DIR="performance_results"
LATEST_RESULTS=$(ls -t "$RESULTS_DIR"/benchmark_*.csv 2>/dev/null | head -1)
LATEST_METRICS=$(ls -t "$RESULTS_DIR"/metrics_*.csv 2>/dev/null | head -1)

# Performance thresholds (minimum acceptable values)
MIN_RPS=20                    # Minimum requests per second
MAX_MEMORY_MB=200            # Maximum memory usage in MB
MAX_CPU_PERCENT=50           # Maximum CPU usage percentage
MIN_SUCCESS_RATE=95          # Minimum success rate percentage

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
        "PASS")
            echo -e "${GREEN}✓ PASS: $message${NC}"
            ;;
        "FAIL")
            echo -e "${RED}✗ FAIL: $message${NC}"
            ;;
        "INFO")
            echo -e "${YELLOW}ℹ INFO: $message${NC}"
            ;;
    esac
}

# Function to validate performance results
validate_performance() {
    if [ ! -f "$LATEST_RESULTS" ]; then
        print_status "FAIL" "No performance results found. Run performance benchmark first."
        return 1
    fi
    
    print_status "INFO" "Validating performance results from: $(basename "$LATEST_RESULTS")"
    echo
    
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # Parse CSV and validate each endpoint
    while IFS=, read -r test_name endpoint requests duration rps avg_response_time; do
        if [ "$test_name" != "test_name" ] && [ -n "$rps" ]; then
            # Clean up RPS value (remove any non-numeric characters)
            clean_rps=$(echo "$rps" | grep -o '[0-9.]*' | head -1)
            
            if [ -n "$clean_rps" ] && [ "$(echo "$clean_rps > 0" | bc -l 2>/dev/null || echo 0)" -eq 1 ]; then
                total_tests=$((total_tests + 1))
                
                # Check if RPS meets minimum threshold
                if [ "$(echo "$clean_rps >= $MIN_RPS" | bc -l 2>/dev/null || echo 0)" -eq 1 ]; then
                    print_status "PASS" "$endpoint ($requests req): $clean_rps req/sec (>= $MIN_RPS req/sec)"
                    passed_tests=$((passed_tests + 1))
                else
                    print_status "FAIL" "$endpoint ($requests req): $clean_rps req/sec (< $MIN_RPS req/sec)"
                    failed_tests=$((failed_tests + 1))
                fi
            fi
        fi
    done < "$LATEST_RESULTS"
    
    echo
    print_status "INFO" "Performance Tests: $passed_tests passed, $failed_tests failed, $total_tests total"
    
    return $failed_tests
}

# Function to validate system metrics
validate_system_metrics() {
    if [ ! -f "$LATEST_METRICS" ]; then
        print_status "FAIL" "No system metrics found. Run performance benchmark first."
        return 1
    fi
    
    print_status "INFO" "Validating system metrics from: $(basename "$LATEST_METRICS")"
    echo
    
    local metric_failures=0
    
    # Parse metrics CSV
    while IFS=, read -r metric_name value; do
        if [ "$metric_name" != "metric_name" ] && [ -n "$value" ]; then
            case "$metric_name" in
                "post_test_jvm_memory_bytes")
                    memory_mb=$(echo "scale=2; $value / 1024 / 1024" | bc -l 2>/dev/null || echo "0")
                    if [ "$(echo "$memory_mb <= $MAX_MEMORY_MB" | bc -l 2>/dev/null || echo 0)" -eq 1 ]; then
                        print_status "PASS" "JVM Memory Usage: ${memory_mb} MB (<= $MAX_MEMORY_MB MB)"
                    else
                        print_status "FAIL" "JVM Memory Usage: ${memory_mb} MB (> $MAX_MEMORY_MB MB)"
                        metric_failures=$((metric_failures + 1))
                    fi
                    ;;
                "post_test_cpu_usage")
                    cpu_percent=$(echo "scale=2; $value * 100" | bc -l 2>/dev/null || echo "0")
                    if [ "$(echo "$cpu_percent <= $MAX_CPU_PERCENT" | bc -l 2>/dev/null || echo 0)" -eq 1 ]; then
                        print_status "PASS" "CPU Usage: ${cpu_percent}% (<= $MAX_CPU_PERCENT%)"
                    else
                        print_status "FAIL" "CPU Usage: ${cpu_percent}% (> $MAX_CPU_PERCENT%)"
                        metric_failures=$((metric_failures + 1))
                    fi
                    ;;
                "post_test_http_requests_count")
                    if [ "$value" -gt 0 ]; then
                        print_status "PASS" "HTTP Requests Processed: $value requests"
                    else
                        print_status "FAIL" "HTTP Requests Processed: $value requests (no traffic processed)"
                        metric_failures=$((metric_failures + 1))
                    fi
                    ;;
            esac
        fi
    done < "$LATEST_METRICS"
    
    return $metric_failures
}

# Function to check service health
validate_service_health() {
    print_status "INFO" "Validating service health after performance testing"
    echo
    
    local services=("localhost:8080" "localhost:8081" "localhost:8082" "localhost:8083")
    local health_failures=0
    
    for service in "${services[@]}"; do
        if curl -s --max-time 5 "http://$service/actuator/health" | grep -q '"status":"UP"'; then
            print_status "PASS" "Service $service is healthy"
        else
            print_status "FAIL" "Service $service is not healthy"
            health_failures=$((health_failures + 1))
        fi
    done
    
    return $health_failures
}

# Main validation function
main() {
    echo "Validating Spring Boot 3.5.5 migration performance..."
    echo "Thresholds: Min RPS=$MIN_RPS, Max Memory=${MAX_MEMORY_MB}MB, Max CPU=${MAX_CPU_PERCENT}%"
    echo
    
    local total_failures=0
    
    # Validate performance results
    echo "1. Performance Validation"
    echo "========================"
    validate_performance
    total_failures=$((total_failures + $?))
    
    echo
    echo "2. System Metrics Validation"
    echo "============================"
    validate_system_metrics
    total_failures=$((total_failures + $?))
    
    echo
    echo "3. Service Health Validation"
    echo "============================"
    validate_service_health
    total_failures=$((total_failures + $?))
    
    echo
    echo "=== Performance Validation Summary ==="
    
    if [ $total_failures -eq 0 ]; then
        print_status "PASS" "All performance validations passed!"
        print_status "INFO" "Spring Boot 3.5.5 migration shows no performance regressions"
        echo
        echo "✅ Migration is APPROVED from performance perspective"
        return 0
    else
        print_status "FAIL" "$total_failures validation(s) failed"
        print_status "INFO" "Review performance issues before proceeding with migration"
        echo
        echo "❌ Migration requires ATTENTION due to performance issues"
        return 1
    fi
}

# Install bc if not available (for calculations)
if ! command -v bc >/dev/null 2>&1; then
    print_status "INFO" "Installing bc for calculations..."
    if command -v brew >/dev/null 2>&1; then
        brew install bc >/dev/null 2>&1 || true
    fi
fi

# Run main validation
main "$@"