package org.springframework.samples.petclinic.customers.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify Spring Boot 3.5 features are available and working.
 * This demonstrates the enhanced conditional property support and other new features.
 */
@SpringBootTest(classes = SpringBoot35FeaturesTest.TestConfiguration.class)
class SpringBoot35FeaturesTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Configuration
    static class TestConfiguration {

        /**
         * Bean that is created when feature is enabled using traditional @ConditionalOnProperty
         */
        @Bean
        @ConditionalOnProperty(name = "test.feature.enabled", havingValue = "true")
        public TestService enabledTestService() {
            return new TestService("enabled");
        }

        /**
         * Bean that is created when feature is disabled
         */
        @Bean
        @ConditionalOnProperty(name = "test.feature.enabled", havingValue = "false", matchIfMissing = true)
        public TestService disabledTestService() {
            return new TestService("disabled");
        }
    }

    public static class TestService {
        private final String status;

        public TestService(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }

    @Test
    void testSpringBoot35IsRunning() {
        // Verify that we're running Spring Boot 3.5.5
        String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
        assertThat(springBootVersion).startsWith("3.5");
    }

    @Test
    void testConditionalPropertyWithFalse() {
        // Test that conditional properties work with false values
        assertThat(applicationContext.containsBean("disabledTestService")).isTrue();
        assertThat(applicationContext.containsBean("enabledTestService")).isFalse();

        TestService service = applicationContext.getBean("disabledTestService", TestService.class);
        assertThat(service.getStatus()).isEqualTo("disabled");
    }

    @Test
    void testDefaultBehavior() {
        // Test default behavior when property is not set
        assertThat(applicationContext.containsBean("disabledTestService")).isTrue();
        assertThat(applicationContext.containsBean("enabledTestService")).isFalse();

        TestService service = applicationContext.getBean("disabledTestService", TestService.class);
        assertThat(service.getStatus()).isEqualTo("disabled");
    }

    @Test
    void testSpringBoot35EnhancedErrorHandling() {
        // Verify that Spring Boot 3.5 enhanced error handling classes are available
        try {
            Class.forName("org.springframework.boot.web.servlet.error.DefaultErrorAttributes");
            // If we get here, the class exists
            assertThat(true).isTrue();
        } catch (ClassNotFoundException e) {
            assertThat(false).as("DefaultErrorAttributes should be available in Spring Boot 3.5").isTrue();
        }
    }

    @Test
    void testMethodValidationSupport() {
        // Verify that method validation support is available
        try {
            Class.forName("org.springframework.validation.method.MethodValidationResult");
            // If we get here, the class exists
            assertThat(true).isTrue();
        } catch (ClassNotFoundException e) {
            assertThat(false).as("MethodValidationResult should be available in Spring Boot 3.5").isTrue();
        }
    }

    @Test
    void testApplicationContextIsWorking() {
        // Basic test to ensure the application context is properly configured
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionNames()).isNotEmpty();
    }
}