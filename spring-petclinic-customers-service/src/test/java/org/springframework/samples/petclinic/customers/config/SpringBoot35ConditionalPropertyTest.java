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
 * Test class to verify Spring Boot 3.5 conditional property features.
 */
@SpringBootTest(classes = SpringBoot35ConditionalPropertyTest.TestConfiguration.class)
@TestPropertySource(properties = "test.feature.enabled=false")
class SpringBoot35ConditionalPropertyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Configuration
    static class TestConfiguration {

        @Bean
        @ConditionalOnProperty(name = "test.feature.enabled", havingValue = "true")
        public TestService enabledTestService() {
            return new TestService("enabled");
        }

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
    void testConditionalPropertyWithFalse() {
        // Test that conditional properties work with false values
        assertThat(applicationContext.containsBean("disabledTestService")).isTrue();
        assertThat(applicationContext.containsBean("enabledTestService")).isFalse();

        TestService service = applicationContext.getBean("disabledTestService", TestService.class);
        assertThat(service.getStatus()).isEqualTo("disabled");
    }
}