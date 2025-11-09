package org.springframework.samples.petclinic.customers.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class to verify Spring Boot 3.5 enhanced error handling with MethodValidationResult.
 * Tests that validation errors are properly included in ErrorAttributes and JSON serialization works correctly.
 */
@WebMvcTest({MethodValidationTestController.class, TestErrorController.class})
@Import(MethodValidationErrorHandlingTest.TestConfig.class)
class MethodValidationErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ErrorAttributes errorAttributes() {
            return new DefaultErrorAttributes();
        }
    }

    @Test
    void testPathVariableValidationError() throws Exception {
        // Test with invalid path variable (negative number)
        MvcResult result = mockMvc.perform(get("/test/validation/owner/{ownerId}", -1))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode errorResponse = objectMapper.readTree(responseBody);

        // Verify basic error structure
        assertThat(errorResponse.has("timestamp")).isTrue();
        assertThat(errorResponse.has("status")).isTrue();
        assertThat(errorResponse.get("status").asInt()).isEqualTo(400);
        assertThat(errorResponse.has("error")).isTrue();
        assertThat(errorResponse.get("error").asText()).isEqualTo("Bad Request");

        // Verify that method validation error details are included
        assertThat(errorResponse.has("message")).isTrue();
        String message = errorResponse.get("message").asText();
        assertThat(message).contains("Owner ID must be positive");

        // Verify path information
        assertThat(errorResponse.has("path")).isTrue();
        assertThat(errorResponse.get("path").asText()).contains("/test/validation/owner/-1");
    }

    @Test
    void testRequestParameterValidationError() throws Exception {
        // Test with invalid request parameters
        MvcResult result = mockMvc.perform(get("/test/validation/search")
                        .param("term", "") // blank term
                        .param("page", "-1") // negative page
                        .param("size", "0")) // invalid size
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode errorResponse = objectMapper.readTree(responseBody);

        // Verify error structure
        assertThat(errorResponse.get("status").asInt()).isEqualTo(400);
        assertThat(errorResponse.get("error").asText()).isEqualTo("Bad Request");

        // Verify that multiple validation errors are captured
        String message = errorResponse.get("message").asText();
        assertThat(message).containsAnyOf(
                "Search term cannot be blank",
                "Page number must be non-negative",
                "Page size must be positive"
        );
    }

    @Test
    void testMultipleConstraintValidationError() throws Exception {
        // Test with multiple validation constraint violations
        MvcResult result = mockMvc.perform(post("/test/validation/validate-multiple")
                        .param("name", "A") // too short
                        .param("age", "16")) // too young
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode errorResponse = objectMapper.readTree(responseBody);

        // Verify error structure
        assertThat(errorResponse.get("status").asInt()).isEqualTo(400);

        // Verify that validation error messages are included
        String message = errorResponse.get("message").asText();
        assertThat(message).containsAnyOf(
                "Name must be between 2 and 50 characters",
                "Age must be at least 18"
        );
    }

    @Test
    void testValidRequestSucceeds() throws Exception {
        // Test that valid requests work properly
        mockMvc.perform(get("/test/validation/owner/{ownerId}", 1))
                .andExpect(status().isOk())
                .andExpect(content().string("Owner ID: 1"));

        mockMvc.perform(get("/test/validation/search")
                        .param("term", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Searching for 'test', page 0, size 10"));

        mockMvc.perform(post("/test/validation/validate-multiple")
                        .param("name", "John Doe")
                        .param("age", "25"))
                .andExpect(status().isOk())
                .andExpect(content().string("Name: John Doe, Age: 25"));
    }

    @Test
    void testErrorAttributesContainMethodValidationResult() throws Exception {
        // Test that ErrorAttributes properly include MethodValidationResult information
        MvcResult result = mockMvc.perform(get("/test/validation/owner/{ownerId}", 0))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode errorResponse = objectMapper.readTree(responseBody);

        // Verify that the error response is properly serialized JSON
        assertThat(errorResponse.isObject()).isTrue();
        assertThat(errorResponse.has("timestamp")).isTrue();
        assertThat(errorResponse.has("status")).isTrue();
        assertThat(errorResponse.has("error")).isTrue();
        assertThat(errorResponse.has("message")).isTrue();
        assertThat(errorResponse.has("path")).isTrue();

        // Verify that the message contains validation-specific information
        String message = errorResponse.get("message").asText();
        assertThat(message).isNotEmpty();
        assertThat(message).contains("Owner ID must be positive");
    }

    @Test
    void testJsonSerializationSafety() throws Exception {
        // Test that complex validation errors are safely serialized to JSON
        MvcResult result = mockMvc.perform(get("/test/validation/search")
                        .param("term", "")
                        .param("page", "-5")
                        .param("size", "0")) // invalid size
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        
        // Verify that the response is valid JSON
        JsonNode errorResponse = objectMapper.readTree(responseBody);
        assertThat(errorResponse).isNotNull();
        
        // Verify that all expected fields are present and properly typed
        assertThat(errorResponse.get("timestamp").isTextual()).isTrue();
        assertThat(errorResponse.get("status").isNumber()).isTrue();
        assertThat(errorResponse.get("error").isTextual()).isTrue();
        assertThat(errorResponse.get("message").isTextual()).isTrue();
        assertThat(errorResponse.get("path").isTextual()).isTrue();
        
        // Verify that the JSON doesn't contain any serialization artifacts
        assertThat(responseBody).doesNotContain("@");
        assertThat(responseBody).doesNotContain("class");
        assertThat(responseBody).doesNotContain("hashCode");
    }
}