package com.adobe.romannumeral.web.error;

import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.exception.InvalidRangeException;
import com.adobe.romannumeral.infrastructure.observability.ConversionMetrics;
import com.adobe.romannumeral.web.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler — maps all exceptions to structured JSON error responses.
 *
 * <p>Ensures every error path returns consistent JSON format, never Spring's default
 * Whitelabel error page or raw stack traces. Records error metrics via
 * {@link ConversionMetrics} for domain exceptions (visible in Grafana).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ConversionMetrics metrics;

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex) {
        log.warn("Invalid input: {}", ex.getMessage());
        metrics.recordError("InvalidInput");
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRange(InvalidRangeException ex) {
        log.warn("Invalid range: {}", ex.getMessage());
        metrics.recordError("InvalidRange");
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.getReasonPhrase(), message, status.value()));
    }
}
