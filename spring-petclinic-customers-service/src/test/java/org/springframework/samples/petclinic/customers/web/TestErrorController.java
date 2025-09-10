package org.springframework.samples.petclinic.customers.web;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Test error controller to demonstrate Spring Boot 3.5 enhanced error handling.
 * This controller shows how MethodValidationResult errors are included in ErrorAttributes.
 */
@RestController
public class TestErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    public TestErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> error(WebRequest webRequest) {
        Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE, 
                                       ErrorAttributeOptions.Include.BINDING_ERRORS,
                                       ErrorAttributeOptions.Include.EXCEPTION));
        
        HttpStatus status = getStatus(body);
        return new ResponseEntity<>(body, status);
    }

    private HttpStatus getStatus(Map<String, Object> errorAttributes) {
        Integer statusCode = (Integer) errorAttributes.get("status");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}