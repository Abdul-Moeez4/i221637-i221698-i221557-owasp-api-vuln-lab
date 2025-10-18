package edu.nu.owaspapivulnlab.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Q8 FIX: Global Exception Handler for Secure Error Responses
 * 
 * OWASP API Security Top 10 - API7: Security Misconfiguration
 * 
 * This handler ensures that:
 * 1. No sensitive information (stack traces, internal details) is exposed to clients
 * 2. All errors have unique tracking IDs for support purposes
 * 3. Generic error messages are returned to prevent information disclosure
 * 4. Detailed errors are logged server-side for debugging
 * 
 * Security Benefits:
 * - Prevents information disclosure through error messages
 * - Provides error tracking without exposing internals
 * - Maintains user experience with helpful but safe error responses
 */
@RestControllerAdvice
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorId = UUID.randomUUID().toString();
        
        // Q8 FIX: Log detailed error server-side with unique ID
        logger.error("🔥 VALIDATION ERROR HANDLER TRIGGERED [{}]: {}", errorId, ex.getMessage());
        System.out.println("🔥 GlobalExceptionHandler: Handling validation error with ID: " + errorId);
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("fields", fieldErrors);
        response.put("errorId", errorId);  // Q8 FIX: Reference ID for support
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorId = UUID.randomUUID().toString();
        
        // Q8 FIX: Log detailed error server-side
        logger.warn("Invalid argument [{}]: {}", errorId, ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Invalid request parameters");
        response.put("errorId", errorId);
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String errorId = UUID.randomUUID().toString();
        
        // Q8 FIX: Log detailed error server-side, return generic message to client
        logger.error("🔥 RUNTIME ERROR HANDLER TRIGGERED [{}]: {}", errorId, ex.getMessage(), ex);
        System.out.println("🔥 GlobalExceptionHandler: Handling runtime error with ID: " + errorId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An error occurred while processing your request");
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        
        // Q8 FIX: Log all unexpected errors with full stack trace
        logger.error("Unexpected error [{}]: {}", errorId, ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "An unexpected error occurred");
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}