package com.adobe.romannumeral.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

/**
 * Shared utility for writing structured JSON error responses from servlet filters.
 *
 * <p>Filters run before the controller — {@code GlobalExceptionHandler} can't catch
 * their errors. This utility ensures 401 and 429 responses use the same JSON format
 * as all other error responses: {@code {"error": "...", "message": "...", "status": N}}
 */
public final class FilterResponseHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FilterResponseHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Writes a structured JSON error response to the HttpServletResponse.
     */
    public static void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(MAPPER.writeValueAsString(Map.of(
                "error", status.getReasonPhrase(),
                "message", message,
                "status", status.value()
        )));
    }
}
