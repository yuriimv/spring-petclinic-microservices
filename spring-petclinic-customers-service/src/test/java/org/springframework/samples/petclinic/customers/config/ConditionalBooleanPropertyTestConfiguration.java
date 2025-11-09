package org.springframework.samples.petclinic.customers.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration to demonstrate Spring Boot 3.5 @ConditionalOnBooleanProperty annotation.
 * This annotation provides a more convenient way to conditionally create beans based on boolean properties.
 */
@Configuration
public class ConditionalBooleanPropertyTestConfiguration {

    /**
     * Bean that is created when feature.enhanced-validation is true
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "feature.enhanced-validation", havingValue = true)
    public ValidationService enhancedValidationService() {
        return new EnhancedValidationService();
    }

    /**
     * Bean that is created when feature.enhanced-validation is false (default behavior)
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "feature.enhanced-validation", havingValue = false, matchIfMissing = true)
    public ValidationService basicValidationService() {
        return new BasicValidationService();
    }

    /**
     * Bean that is created when feature.caching is enabled (defaults to true if missing)
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "feature.caching", havingValue = true, matchIfMissing = true)
    public CacheService cacheService() {
        return new CacheService();
    }

    /**
     * Bean that is created when feature.metrics is explicitly enabled
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "feature.metrics", havingValue = true)
    public MetricsService metricsService() {
        return new MetricsService();
    }

    /**
     * Bean that is created when feature.debug-mode is disabled
     */
    @Bean
    @ConditionalOnBooleanProperty(name = "feature.debug-mode", havingValue = false, matchIfMissing = true)
    public ProductionService productionService() {
        return new ProductionService();
    }

    // Test service interfaces and implementations
    public interface ValidationService {
        String getType();
    }

    public static class EnhancedValidationService implements ValidationService {
        @Override
        public String getType() {
            return "enhanced";
        }
    }

    public static class BasicValidationService implements ValidationService {
        @Override
        public String getType() {
            return "basic";
        }
    }

    public static class CacheService {
        public String getStatus() {
            return "enabled";
        }
    }

    public static class MetricsService {
        public String getStatus() {
            return "collecting";
        }
    }

    public static class ProductionService {
        public String getMode() {
            return "production";
        }
    }
}