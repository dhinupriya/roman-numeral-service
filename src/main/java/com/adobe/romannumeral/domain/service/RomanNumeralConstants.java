package com.adobe.romannumeral.domain.service;

/**
 * Domain constants for Roman numeral conversion.
 *
 * <p>The standard form of Roman numerals supports values from 1 to 3999.
 * Values beyond 3999 require vinculum notation (overline bars), which is
 * outside the scope of standard form.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Roman_numerals#Standard_form">Wikipedia: Roman Numerals — Standard Form</a>
 */
public final class RomanNumeralConstants {

    /** Minimum supported value for Roman numeral conversion. */
    public static final int MIN_VALUE = 1;

    /** Maximum supported value for Roman numeral conversion (standard form). */
    public static final int MAX_VALUE = 3999;

    private RomanNumeralConstants() {
        // Prevent instantiation — constants-only class
    }
}
