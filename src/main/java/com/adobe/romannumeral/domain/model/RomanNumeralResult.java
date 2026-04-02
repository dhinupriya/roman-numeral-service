package com.adobe.romannumeral.domain.model;

import java.util.Objects;

/**
 * Value Object representing a single Roman numeral conversion result.
 *
 * <p>Immutable and self-validating — rejects null values at construction time (fail-fast).
 * Thread-safe by virtue of immutability, safe to share across virtual threads.
 *
 * <p>Both fields are strings to match the assessment-required JSON format:
 * {@code {"input": "1", "output": "I"}}
 *
 * @param input  the original integer as a string (e.g., "1994")
 * @param output the Roman numeral representation (e.g., "MCMXCIV")
 */
public record RomanNumeralResult(String input, String output) {

    public RomanNumeralResult {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(output, "output must not be null");
    }
}
