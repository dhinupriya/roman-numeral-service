package com.adobe.romannumeral.web.dto;

/**
 * Structured error response DTO.
 *
 * <p>The assessment allows plain text errors, but structured JSON is better practice:
 * {@code {"error": "Bad Request", "message": "Number must be between 1 and 3999", "status": 400}}
 *
 * @param error   the HTTP status reason phrase (e.g., "Bad Request")
 * @param message a human-readable description of what went wrong
 * @param status  the HTTP status code
 */
public record ErrorResponse(String error, String message, int status) {

    public static ErrorResponse of(String error, String message, int status) {
        return new ErrorResponse(error, message, status);
    }
}
