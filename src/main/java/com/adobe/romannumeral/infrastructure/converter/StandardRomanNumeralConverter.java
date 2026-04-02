package com.adobe.romannumeral.infrastructure.converter;

import com.adobe.romannumeral.domain.exception.InvalidInputException;
import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConstants;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import org.springframework.stereotype.Component;

/**
 * Core Roman numeral conversion algorithm — hand-written, no libraries.
 *
 * <p>Uses the <b>descending value-table</b> approach: two parallel arrays of values and symbols,
 * ordered from largest to smallest. For each value, subtract it from the input as many times as
 * possible, appending the corresponding symbol each time.
 *
 * <p>The 13-entry table covers all standard symbols (I, V, X, L, C, D, M) plus the 6 subtractive
 * forms (IV, IX, XL, XC, CD, CM). This eliminates the need for special-case logic — subtractive
 * notation is handled naturally by the table ordering.
 *
 * <p><b>Complexity</b>: O(1) — the loop iterates at most 15 times (for 3888 = MMMDCCCLXXXVIII),
 * regardless of input size. The number of iterations is bounded by the value table, not the input.
 *
 * <p><b>Thread safety</b>: This class is stateless — all state lives on the call stack
 * (method-local variables). Safe to call from multiple virtual threads concurrently.
 *
 * <p>This converter is used directly for <b>range queries</b> where the assessment requires
 * parallel computation. For single queries, {@code CachedRomanNumeralConverter} provides
 * O(1) lookup by pre-computing all values at startup using this converter.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Roman_numerals#Standard_form">Wikipedia: Roman Numerals — Standard Form</a>
 */
@Component
public class StandardRomanNumeralConverter implements RomanNumeralConverter {

    /**
     * Descending value table — maps integer values to their Roman numeral symbols.
     * Includes both standard symbols and subtractive forms, ordered largest-first.
     * The parallel arrays avoid the overhead of a Map and make the algorithm a simple loop.
     */
    private static final int[] VALUES = {
            1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    };

    private static final String[] SYMBOLS = {
            "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    };

    @Override
    public RomanNumeralResult convert(int number) {
        validateInput(number);

        StringBuilder roman = new StringBuilder();
        int remaining = number;

        // Greedily subtract the largest possible value at each step.
        // The subtractive forms (CM, CD, XC, XL, IX, IV) are in the table,
        // so they are handled naturally without special-case branching.
        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                roman.append(SYMBOLS[i]);
                remaining -= VALUES[i];
            }
        }

        return new RomanNumeralResult(String.valueOf(number), roman.toString());
    }

    private void validateInput(int number) {
        if (number < RomanNumeralConstants.MIN_VALUE || number > RomanNumeralConstants.MAX_VALUE) {
            throw new InvalidInputException(
                    "Number must be between " + RomanNumeralConstants.MIN_VALUE
                            + " and " + RomanNumeralConstants.MAX_VALUE + ", got: " + number);
        }
    }
}
