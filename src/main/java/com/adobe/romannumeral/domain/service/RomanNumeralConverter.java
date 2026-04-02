package com.adobe.romannumeral.domain.service;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;

/**
 * Port (interface) for Roman numeral conversion — the core domain contract.
 *
 * <p>This is a Strategy pattern interface with two implementations:
 * <ul>
 *   <li>{@code StandardRomanNumeralConverter} — the algorithm, used for range queries
 *       where parallel computation is required</li>
 *   <li>{@code CachedRomanNumeralConverter} — pre-computed O(1) lookup, used for
 *       single queries (wraps StandardConverter via Decorator pattern)</li>
 * </ul>
 *
 * <p>Defined in the domain layer with zero framework dependencies — can be tested
 * with {@code new StandardRomanNumeralConverter()} without any Spring context.
 */
public interface RomanNumeralConverter {

    /**
     * Converts an integer to its Roman numeral representation.
     *
     * @param number the integer to convert, must be in range
     *               [{@link RomanNumeralConstants#MIN_VALUE}, {@link RomanNumeralConstants#MAX_VALUE}]
     * @return the conversion result containing both the input and output as strings
     * @throws com.adobe.romannumeral.domain.exception.InvalidInputException
     *         if the number is outside the supported range
     */
    RomanNumeralResult convert(int number);
}
