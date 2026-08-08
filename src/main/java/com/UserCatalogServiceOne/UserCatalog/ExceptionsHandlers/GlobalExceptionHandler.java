package com.UserCatalogServiceOne.UserCatalog.ExceptionsHandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 🟢 1. CATCHES USER MISTAKES: Returns clean JSON to frontend, NO STACK TRACE in logs
    @ExceptionHandler(ClientValidationException.class)
    public ResponseEntity<Map<String, String>> handleClientValidation(ClientValidationException ex) {
        log.info("User Input Rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // 🟢 2. CATCHES LOGIN FAILURES: Wrong Password
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        log.info("Login Failed: Invalid password provided.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid password provided."));
    }

    // 🟢 3. CATCHES LOGIN FAILURES: User doesn't exist
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UsernameNotFoundException ex) {
        log.info("Login Failed: User does not exist.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User does not exist. Please register first."));
    }

    // 🟢 4. CATCHES DTO VALIDATION FAILURES (@Valid annotations)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage); // e.g., {"username": "Username cannot be empty"}
        });

        log.info("DTO Validation Failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 🔴 5. CATCHES REAL SYSTEM CRASHES: Prints the full stack trace to logs
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleSystemException(Exception ex) {
        log.error("🚨 CRITICAL SYSTEM FAILURE 🚨", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An internal system error occurred."));
    }
}