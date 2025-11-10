package org.springframework.samples.petclinic.api.boundary.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify Spring Boot 3.5 enhanced error handling works in the API Gateway.
 * Tests that method validation errors are properly handled and serialized in the gateway layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({ApiGatewayMethodValidationTest.TestValidationController.class, ApiGatewayMethodValidationTest.ValidationExceptionHandler.class})
class ApiGatewayMethodValidationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomersServiceClient customersServiceClient;

    @MockBean
    private VisitsServiceClient visitsServiceClient;

    /**
     * Test controller to simulate method validation in API Gateway context
     */
    @RestController
    @RequestMapping("/api/gateway/test")
    @Validated
    static class TestValidationController {

        @GetMapping("/owner/{ownerId}")
        public String getOwnerValidation(@PathVariable @Min(value = 1, message = "Owner ID must be positive in gateway") Integer ownerId) {
            return "Gateway validation passed for owner: " + ownerId;
        }
    }

    /**
     * Exception handler for method validation errors in WebFlux
     */
    @RestControllerAdvice
    static class ValidationExceptionHandler {

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
                ConstraintViolationException ex, ServerWebExchange exchange) {

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("timestamp", Instant.now().toString());
            errorResponse.put("path", exchange.getRequest().getPath().value());
            errorResponse.put("status", 400);
            errorResponse.put("error", "Bad Request");

            // Combine all constraint violation messages
            String message = ex.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            errorResponse.put("message", message);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @Test
    void testApiGatewayMethodValidationErrorHandling() throws Exception {
        // Test method validation error in API Gateway context
        String responseBody = webTestClient.get()
                .uri("/api/gateway/test/owner/{ownerId}", -1)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode errorResponse = objectMapper.readTree(responseBody);

        // Verify error structure is consistent with Spring Boot 3.5 enhanced error handling
        assertThat(errorResponse.has("timestamp")).isTrue();
        assertThat(errorResponse.has("status")).isTrue();
        assertThat(errorResponse.get("status").asInt()).isEqualTo(400);
        assertThat(errorResponse.has("error")).isTrue();
        assertThat(errorResponse.get("error").asText()).isEqualTo("Bad Request");

        // Verify that method validation error message is included
        assertThat(errorResponse.has("message")).isTrue();
        String message = errorResponse.get("message").asText();
        assertThat(message).contains("Owner ID must be positive in gateway");

        // Verify path information
        assertThat(errorResponse.has("path")).isTrue();
        assertThat(errorResponse.get("path").asText()).contains("/api/gateway/test/owner/-1");
    }

    @Test
    void testApiGatewayValidRequestSucceeds() throws Exception {
        // Test that valid requests work properly in API Gateway
        webTestClient.get()
                .uri("/api/gateway/test/owner/{ownerId}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Gateway validation passed for owner: 1");
    }

    @Test
    void testApiGatewayErrorResponseJsonSerialization() throws Exception {
        // Test that error responses are properly serialized in API Gateway context
        String responseBody = webTestClient.get()
                .uri("/api/gateway/test/owner/{ownerId}", 0)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        
        // Verify that the response is valid JSON
        JsonNode errorResponse = objectMapper.readTree(responseBody);
        assertThat(errorResponse).isNotNull();
        
        // Verify JSON structure and content safety
        assertThat(errorResponse.get("timestamp").isTextual()).isTrue();
        assertThat(errorResponse.get("status").isNumber()).isTrue();
        assertThat(errorResponse.get("error").isTextual()).isTrue();
        assertThat(errorResponse.get("message").isTextual()).isTrue();
        assertThat(errorResponse.get("path").isTextual()).isTrue();
        
        // Verify no serialization artifacts
        assertThat(responseBody).doesNotContain("@");
        assertThat(responseBody).doesNotContain("class");
        assertThat(responseBody).doesNotContain("hashCode");
        
        // Verify the error message contains validation information
        String message = errorResponse.get("message").asText();
        assertThat(message).isNotEmpty();
        assertThat(message).contains("Owner ID must be positive in gateway");
    }
}