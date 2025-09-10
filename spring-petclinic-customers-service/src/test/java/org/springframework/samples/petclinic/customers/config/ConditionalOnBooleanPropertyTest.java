package org.springframework.samples.petclinic.customers.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify Spring Boot 3.5 @ConditionalOnBooleanProperty annotation functionality.
 * Tests various scenarios of conditional bean creation based on boolean properties.
 */
class ConditionalOnBooleanPropertyTest {

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
    static class EnhancedValidationEnabledTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testEnhancedValidationServiceIsCreated() {
            // Verify that enhanced validation service is created when property is true
            assertThat(applicationContext.containsBean("enhancedValidationService")).isTrue();
            assertThat(applicationContext.containsBean("basicValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("enhancedValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("enhanced");
        }

        @Test
        void testCacheServiceIsCreated() {
            // Verify that cache service is created when property is true
            assertThat(applicationContext.containsBean("cacheService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.CacheService cacheService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.CacheService.class);
            assertThat(cacheService.getStatus()).isEqualTo("enabled");
        }

        @Test
        void testMetricsServiceIsCreated() {
            // Verify that metrics service is created when explicitly enabled
            assertThat(applicationContext.containsBean("metricsService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.MetricsService metricsService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.MetricsService.class);
            assertThat(metricsService.getStatus()).isEqualTo("collecting");
        }

        @Test
        void testProductionServiceIsCreated() {
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
    static class EnhancedValidationDisabledTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testBasicValidationServiceIsCreated() {
            // Verify that basic validation service is created when property is false
            assertThat(applicationContext.containsBean("basicValidationService")).isTrue();
            assertThat(applicationContext.containsBean("enhancedValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("basicValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("basic");
        }

        @Test
        void testCacheServiceIsNotCreated() {
            // Verify that cache service is not created when property is false
            assertThat(applicationContext.containsBean("cacheService")).isFalse();
        }

        @Test
        void testMetricsServiceIsNotCreated() {
            // Verify that metrics service is not created when not explicitly enabled
            assertThat(applicationContext.containsBean("metricsService")).isFalse();
        }

        @Test
        void testProductionServiceIsNotCreated() {
            // Verify that production service is not created when debug mode is true
            assertThat(applicationContext.containsBean("productionService")).isFalse();
        }
    }

    /**
     * Test with default values (no properties set)
     */
    @SpringBootTest(classes = ConditionalBooleanPropertyTestConfiguration.class)
    static class DefaultValuesTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testDefaultBehaviorWithMatchIfMissing() {
            // Verify that basic validation service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("basicValidationService")).isTrue();
            assertThat(applicationContext.containsBean("enhancedValidationService")).isFalse();

            ConditionalBooleanPropertyTestConfiguration.ValidationService validationService = 
                applicationContext.getBean("basicValidationService", ConditionalBooleanPropertyTestConfiguration.ValidationService.class);
            assertThat(validationService.getType()).isEqualTo("basic");
        }

        @Test
        void testCacheServiceCreatedByDefault() {
            // Verify that cache service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("cacheService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.CacheService cacheService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.CacheService.class);
            assertThat(cacheService.getStatus()).isEqualTo("enabled");
        }

        @Test
        void testMetricsServiceNotCreatedByDefault() {
            // Verify that metrics service is not created by default (no matchIfMissing)
            assertThat(applicationContext.containsBean("metricsService")).isFalse();
        }

        @Test
        void testProductionServiceCreatedByDefault() {
            // Verify that production service is created by default (matchIfMissing = true)
            assertThat(applicationContext.containsBean("productionService")).isTrue();
            
            ConditionalBooleanPropertyTestConfiguration.ProductionService productionService = 
                applicationContext.getBean(ConditionalBooleanPropertyTestConfiguration.ProductionService.class);
            assertThat(productionService.getMode()).isEqualTo("production");
        }
    }

    /**
     * Test with mixed property values
     */
    @SpringBootTest(classes = ConditionalBooleanPropertyTestConfiguration.class)
    @TestPropertySource(properties = {
            "feature.enhanced-validation=true",
            "feature.caching=false",
            "feature.metrics=false",
            "feature.debug-mode=true"
    })
    static class MixedPropertiesTest {

        @Autowired
        private ApplicationContext applicationContext;

        @Test
        void testMixedPropertyConfiguration() {
            // Enhanced validation should be enabled
            assertThat(applicationContext.containsBean("enhancedValidationService")).isTrue();
            assertThat(applicationContext.containsBean("basicValidationService")).isFalse();

            // Cache should be disabled
            assertThat(applicationContext.containsBean("cacheService")).isFalse();

            // Metrics should be disabled
            assertThat(applicationContext.containsBean("metricsService")).isFalse();

            // Production service should not be created (debug mode is true)
            assertThat(applicationContext.containsBean("productionService")).isFalse();
        }
    }
}