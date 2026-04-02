package com.adobe.romannumeral.domain.exception;

/**
 * Base exception for all domain-level errors.
 *
 * <p>Sealed to create a closed hierarchy — the {@code GlobalExceptionHandler} can
 * exhaustively handle every domain error type. If a new exception is added but not
 * handled, the compiler can warn at the usage site (when using pattern matching).
 *
 * <p>Each permitted subtype maps to exactly one HTTP status code:
 * <ul>
 *   <li>{@link InvalidInputException} → 400 Bad Request</li>
 *   <li>{@link InvalidRangeException} → 400 Bad Request</li>
 * </ul>
 */
public sealed class DomainException extends RuntimeException
        permits InvalidInputException, InvalidRangeException {

    protected DomainException(String message) {
        super(message);
    }
}
