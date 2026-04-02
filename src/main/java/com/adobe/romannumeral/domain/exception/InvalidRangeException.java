package com.adobe.romannumeral.domain.exception;

/**
 * Thrown when a range conversion request is invalid — min ≥ max, values out of range,
 * or range size exceeds the configured maximum.
 */
public final class InvalidRangeException extends DomainException {

    public InvalidRangeException(String message) {
        super(message);
    }
}
