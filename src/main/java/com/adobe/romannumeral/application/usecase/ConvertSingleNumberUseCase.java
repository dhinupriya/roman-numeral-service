package com.adobe.romannumeral.application.usecase;

import com.adobe.romannumeral.domain.model.RomanNumeralResult;
import com.adobe.romannumeral.domain.service.RomanNumeralConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Use case for converting a single integer to a Roman numeral.
 *
 * <p>Delegates to a {@link RomanNumeralConverter} — in production this will be the
 * {@code CachedRomanNumeralConverter} for O(1) lookups. Until Phase 2 introduces
 * the cached converter, this delegates to {@code StandardRomanNumeralConverter}.
 *
 * <p>Single Responsibility: this use case handles only single conversions.
 * Range conversions are handled by {@code ConvertRangeUseCase} (Phase 2).
 */
@Slf4j
@RequiredArgsConstructor
public class ConvertSingleNumberUseCase {

    private final RomanNumeralConverter converter;

    /**
     * Converts a single integer to its Roman numeral representation.
     *
     * @param number the integer to convert
     * @return the conversion result
     * @throws com.adobe.romannumeral.domain.exception.InvalidInputException if out of range
     */
    public RomanNumeralResult execute(int number) {
        log.debug("Converting single number: {}", number);
        return converter.convert(number);
    }
}
