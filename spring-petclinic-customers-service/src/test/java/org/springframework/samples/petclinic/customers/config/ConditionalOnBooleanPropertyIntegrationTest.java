package org.springframework.samples.petclinic.customers.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to verify Spring Boot 3.5 @ConditionalOnBooleanProperty annotation functionality.
 * These tests demonstrate the new annotation's behavior with different property configurations.
 */
class ConditionalOnBooleanPropertyIntegrationTest {

    /**
     * Test with enhanced validation enabled
     */
    @SpringBootTest(classes = ConditionalBooleanPropertyTestConfiguration.class)
    @TestPropertySource(properties = {
            "feature.enhanced-validation=true",
            "feature.caching=true",
            "feature.metrics=true",
            "feature.debug-mode=false"
    })
    static class WhenEnhancedValidationEnabled {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void shouldCreateEnhancedValidationService() {
            // Verify that enhanced validation service is created when property is true
            assertThat(applicationContext.containsBean("enhancedValidationService")).isTrue();
            assertThat(applicationContext.containsBean("basicValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("enhancedValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("enhanced");
        }

        @Test
        void shouldCreateCacheService() {
            // Verify that cache service is created when property is true
            assertThat(applicationContext.containsBean("cacheService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.CacheService cacheService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.CacheService.class);
            assertThat(cacheService.getStatus()).isEqualTo("enabled");
        }

        @Test
        void shouldCreateMetricsService() {
            // Verify that metrics service is created when explicitly enabled
            assertThat(applicationContext.containsBean("metricsService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.MetricsService metricsService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.MetricsService.class);
            assertThat(metricsService.getStatus()).isEqualTo("collecting");
        }

        @Test
        void shouldCreateProductionService() {
            // Verify that production service is created when debug mode is false
            assertThat(applicationContext.containsBean("productionService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.ProductionService productionService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.ProductionService.class);
            assertThat(productionService.getMode()).isEqualTo("production");
        }
    }

    /**
     * Test with enhanced validation disabled
     */
    @SpringBootTest(classes = ConditionalBooleanPropertyTestConfiguration.class)
    @TestPropertySource(properties = {
            "feature.enhanced-validation=false",
            "feature.caching=false",
            "feature.debug-mode=true"
    })
    static class WhenEnhancedValidationDisabled {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void shouldCreateBasicValidationService() {
            // Verify that basic validation service is created when property is false
            assertThat(applicationContext.containsBean("basicValidationService")).isTrue();
            assertThat(applicationContext.containsBean("enhancedValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("basicValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("basic");
        }

        @Test
        void shouldNotCreateCacheService() {
            // Verify that cache service is not created when property is false
            assertThat(applicationContext.containsBean("cacheService")).isFalse();
        }

        @Test
        void shouldNotCreateMetricsService() {
            // Verify that metrics service is not created when not explicitly enabled
            assertThat(applicationContext.containsBean("metricsService")).isFalse();
        }

        @Test
        void shouldNotCreateProductionService() {
            // Verify that production service is not created when debug mode is true
            assertThat(applicationContext.containsBean("productionService")).isFalse();
        }
    }

    /**
     * Test with default values (no properties set)
     */
    @SpringBootTest(classes = ConditionalBooleanPropertyTestConfiguration.class)
    static class WhenUsingDefaultValues {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void shouldUseDefaultBehaviorWithMatchIfMissing() {
            // Verify that basic validation service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("basicValidationService")).isTrue();
            assertThat(applicationContext.containsBean("enhancedValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("basicValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("basic");
        }

        @Test
        void shouldCreateCacheServiceByDefault() {
            // Verify that cache service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("cacheService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.CacheService cacheService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.CacheService.class);
            assertThat(cacheService.getStatus()).isEqualTo("enabled");
        }

        @Test
        void shouldNotCreateMetricsServiceByDefault() {
            // Verify that metrics service is not created by default (no matchIfMissing)
            assertThat(applicationContext.containsBean("metricsService")).isFalse();
        }

        @Test
        void shouldCreateProductionServiceByDefault() {
            // Verify that production service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("productionService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.ProductionService productionService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.ProductionService.class);
            assertThat(productionService.getMode()).isEqualTo("production");
        }
    }
}