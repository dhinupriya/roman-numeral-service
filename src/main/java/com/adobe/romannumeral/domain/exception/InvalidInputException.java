package com.adobe.romannumeral.domain.exception;

/**
 * Thrown when a single conversion input is invalid — outside the supported range
 * or not a valid integer.
 */
public final class InvalidInputException extends DomainException {

    public InvalidInputException(String message) {
        super(message);
    }
}
