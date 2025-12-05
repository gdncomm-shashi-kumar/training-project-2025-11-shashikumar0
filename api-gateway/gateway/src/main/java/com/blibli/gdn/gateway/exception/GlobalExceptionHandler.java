package com.blibli.gdn.gateway.exception;

import com.blibli.gdn.gateway.model.GatewayErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for API Gateway
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle service unavailable errors
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<GatewayErrorResponse> handleServiceUnavailable(
            ServiceUnavailableException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.error("Service unavailable: {} - traceId: {}", ex.getMessage(), traceId);

        Map<String, Object> details = new HashMap<>();
        details.put("service", ex.getServiceName());

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                request.getRequestURI(),
                traceId,
                details
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle member already exists (409 Conflict)
     */
    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<GatewayErrorResponse> handleMemberAlreadyExists(
            MemberAlreadyExistsException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Member already exists: {} - traceId: {}", ex.getMessage(), traceId);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle invalid credentials (401 Unauthorized)
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<GatewayErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Invalid credentials: {} - traceId: {}", ex.getMessage(), traceId);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle invalid token (401 Unauthorized)
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GatewayErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Invalid token: {} - traceId: {}", ex.getMessage(), traceId);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle member not found (404 Not Found)
     */
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<GatewayErrorResponse> handleMemberNotFound(
            MemberNotFoundException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Member not found: {} - traceId: {}", ex.getMessage(), traceId);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GatewayErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Validation error - traceId: {}", traceId);

        Map<String, Object> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                request.getRequestURI(),
                traceId,
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<GatewayErrorResponse> handleNotFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.warn("Endpoint not found: {} - traceId: {}", request.getRequestURI(), traceId);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "The requested endpoint does not exist",
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GatewayErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected error: {} - traceId: {}", ex.getMessage(), traceId, ex);

        GatewayErrorResponse error = GatewayErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(),
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
