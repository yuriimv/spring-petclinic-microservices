package org.springframework.samples.petclinic.customers.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller to demonstrate method validation with Spring Boot 3.5 enhanced error handling.
 * This controller is used specifically for testing MethodValidationResult integration with ErrorAttributes.
 */
@RestController
@RequestMapping("/test/validation")
@Validated
public class MethodValidationTestController {

    /**
     * Test endpoint with path variable validation
     */
    @GetMapping("/owner/{ownerId}")
    public String getOwnerById(@PathVariable @Min(value = 1, message = "Owner ID must be positive") int ownerId) {
        return "Owner ID: " + ownerId;
    }

    /**
     * Test endpoint with request parameter validation
     */
    @GetMapping("/search")
    public String searchOwners(
            @RequestParam @NotBlank(message = "Search term cannot be blank") String term,
            @RequestParam @Min(value = 0, message = "Page number must be non-negative") int page,
            @RequestParam @Min(value = 1, message = "Page size must be positive") int size) {
        return String.format("Searching for '%s', page %d, size %d", term, page, size);
    }

    /**
     * Test endpoint with multiple validation constraints
     */
    @PostMapping("/validate-multiple")
    public String validateMultiple(
            @RequestParam @NotBlank(message = "Name is required") @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") String name,
            @RequestParam @Min(value = 18, message = "Age must be at least 18") int age) {
        return String.format("Name: %s, Age: %d", name, age);
    }
}