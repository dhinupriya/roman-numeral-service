package com.adobe.romannumeral.domain.model;

import com.adobe.romannumeral.domain.exception.InvalidRangeException;
import com.adobe.romannumeral.domain.service.RomanNumeralConstants;

/**
 * Value Object representing a validated range for batch Roman numeral conversion.
 *
 * <p>Self-validating — the compact constructor enforces all business rules at creation time:
 * <ul>
 *   <li>{@code min} must be less than {@code max} (strict inequality)</li>
 *   <li>Both values must be within [{@code MIN_VALUE}, {@code MAX_VALUE}]</li>
 * </ul>
 *
 * <p>If any constraint is violated, construction fails immediately with
 * {@link InvalidRangeException}. This guarantees that any {@code RomanNumeralRange}
 * instance in the system is always valid — no downstream null checks or re-validation needed.
 *
 * @param min the lower bound of the range (inclusive)
 * @param max the upper bound of the range (inclusive)
 */
public record RomanNumeralRange(int min, int max) {

    public RomanNumeralRange {
        if (min < RomanNumeralConstants.MIN_VALUE || min > RomanNumeralConstants.MAX_VALUE) {
            throw new InvalidRangeException(
                    "min must be between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE + ", got: " + min);
        }
        if (max < RomanNumeralConstants.MIN_VALUE || max > RomanNumeralConstants.MAX_VALUE) {
            throw new InvalidRangeException(
                    "max must be between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE + ", got: " + max);
        }
        if (min >= max) {
            throw new InvalidRangeException(
                    "min must be less than max, got: min=" + min + ", max=" + max);
        }
    }

    /** Returns the number of values in this range (inclusive on both ends). */
    public int size() {
        return max - min + 1;
    }
}
